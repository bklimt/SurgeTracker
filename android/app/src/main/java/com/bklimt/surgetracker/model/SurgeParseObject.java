package com.bklimt.surgetracker.model;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by klimt on 7/5/14.
 */
@ParseClassName("Surge")
public class SurgeParseObject extends ParseObject {
    public static final String PIN_SURGES = "surges";
    public static final String PIN_DELETE = "delete";

    private static final Logger logger = Logger.getLogger(SurgeParseObject.class.getName());

    private static final Task<Void> taskQueue = Task.forResult(null);

    public static Task<List<Surge>> loadAsync() {
        final Task<List<Surge>>.TaskCompletionSource tcs = Task.create();
        ParseQuery<SurgeParseObject> query = ParseQuery.getQuery(SurgeParseObject.class);
        query.fromPin(PIN_SURGES);
        query.findInBackground(new FindCallback<SurgeParseObject>() {
            @Override
            public void done(List<SurgeParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    ArrayList<Surge> surges = new ArrayList<Surge>();
                    for (SurgeParseObject parseObject : parseObjects) {
                        surges.add(new Surge(parseObject));
                    }
                    tcs.setResult(surges);
                } else {
                    tcs.setError(e);
                }
            }
        });
        return tcs.getTask();
    }

    public static Task<Void> syncAsync() {
        return taskQueue.continueWithTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                /*
                 * Make sure a user object exists.
                 */
                logger.info("Getting current user");
                final ParseUser currentUser = ParseUser.getCurrentUser();
                if (currentUser != null && currentUser.getObjectId() != null) {
                    return Task.<Void> forResult(null);
                }
                ParseUser.logOut();

                return Task.forResult(null).onSuccessTask(new Continuation<Object, Task<ParseUser>>() {
                    @Override
                    public Task<ParseUser> then(Task<Object> objectTask) throws Exception {
                        logger.info("Logging in anonymously");
                        final ParseUser user = new ParseUser();
                        user.setUsername(UUID.randomUUID().toString());
                        user.setPassword(UUID.randomUUID().toString());
                        // TODO(klimt): This doesn't seem to work with local datastore.
                        // user.setACL(new ParseACL());
                        final Task<ParseUser>.TaskCompletionSource tcs = Task.create();
                        user.signUpInBackground(new SignUpCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    tcs.setResult(user);
                                } else {
                                    tcs.setError(e);
                                }
                            }
                        });
                        return tcs.getTask();
                    }
                }).onSuccess(new Continuation<ParseUser, Void>() {
                    @Override
                    public Void then(Task<ParseUser> task) throws Exception {
                        ParseUser user = task.getResult();
                        logger.info("Signup was successful: " + user.getObjectId());
                        return null;
                    }
                });
            }
        }).onSuccessTask(new Continuation<Void, Task<List<SurgeParseObject>>>() {
            @Override
            public Task<List<SurgeParseObject>> then(Task<Void> task) throws Exception {
                logger.info("Querying for all pinned surges");
                /*
                 * Query for all the current surges.
                 */
                ParseQuery<SurgeParseObject> query = ParseQuery.getQuery(SurgeParseObject.class);
                query.setLimit(1000);
                query.fromPin(PIN_SURGES);
                final Task<List<SurgeParseObject>>.TaskCompletionSource tcs = Task.create();
                query.findInBackground(new FindCallback<SurgeParseObject>() {
                    @Override
                    public void done(List<SurgeParseObject> surges, ParseException e) {
                        if (e == null) {
                            tcs.setResult(surges);
                        } else {
                            tcs.setError(e);
                        }
                    }
                });
                return tcs.getTask();
            }
        }).onSuccessTask(new Continuation<List<SurgeParseObject>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<SurgeParseObject>> task) throws Exception {
                logger.info("Saving all pinned surges");
                /*
                 * Save all the current surges.
                 */
                List<SurgeParseObject> surges = task.getResult();
                Task<Void> finished = Task.forResult(null);
                for (final SurgeParseObject surge : surges) {
                    finished = finished.onSuccessTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            if (surge.getACL() == null) {
                                surge.setACL(new ParseACL(ParseUser.getCurrentUser()));
                            }
                            return surge.saveAsync();
                        }
                    });
                }
                return finished;
            }
        }).onSuccessTask(new Continuation<Void, Task<List<SurgeParseObject>>>() {
            @Override
            public Task<List<SurgeParseObject>> then(Task<Void> task) throws Exception {
                logger.info("Querying for all surges marked for deletion");
                /*
                 * Get the list of all surges that need to be deleted.
                 */
                ParseQuery<SurgeParseObject> query = ParseQuery.getQuery(SurgeParseObject.class);
                query.setLimit(1000);
                query.fromPin(PIN_DELETE);
                final Task<List<SurgeParseObject>>.TaskCompletionSource tcs = Task.create();
                query.findInBackground(new FindCallback<SurgeParseObject>() {
                    @Override
                    public void done(List<SurgeParseObject> surges, ParseException e) {
                        if (e == null) {
                            tcs.setResult(surges);
                        } else {
                            tcs.setError(e);
                        }
                    }
                });
                return tcs.getTask();
            }
        }).onSuccessTask(new Continuation<List<SurgeParseObject>, Task<Void>>() {
            @Override
            public Task<Void> then(Task<List<SurgeParseObject>> task) throws Exception {
                logger.info("Deleting all surges marked for deletion");
                /*
                 * Delete all the surges pending delete.
                 */
                List<SurgeParseObject> surges = task.getResult();
                Task<Void> finished = Task.forResult(null);
                for (final SurgeParseObject surge : surges) {
                    finished = finished.onSuccessTask(new Continuation<Void, Task<Void>>() {
                        @Override
                        public Task<Void> then(Task<Void> task) throws Exception {
                            return surge.deleteAsync();
                        }
                    });
                }
                return finished;
            }
        });
    }

    /* package */ Task<Void> pinAsync() {
        logger.log(Level.INFO,
                "Pinning Surge with start=" + getString("start") + ", end=" + getString("end"));

        final Task<Void>.TaskCompletionSource tcs = Task.create();
        pinInBackground(PIN_SURGES, new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Pinned successfully.");
                    tcs.setResult(null);
                } else {
                    logger.log(Level.SEVERE, "Unable to pin Surge.", e);
                    tcs.setError(e);
                }
            }
        });
        return tcs.getTask();
    }

    /* package */ Task<Void> saveAsync() {
        final Task<Void>.TaskCompletionSource tcs = Task.create();
        saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Saved successfully.");
                    tcs.setResult(null);
                } else {
                    logger.log(Level.SEVERE, "Unable to save Surge.", e);
                    tcs.setError(e);
                }
            }
        });
        return tcs.getTask();
    }

    /* package */ Task<Void> deleteAsync() {
        final Task<Void>.TaskCompletionSource tcs = Task.create();
        deleteInBackground(new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Deleted successfully.");
                    tcs.setResult(null);
                } else {
                    logger.log(Level.SEVERE, "Unable to delete Surge.", e);
                    tcs.setError(e);
                }
            }
        });
        return tcs.getTask();
    }

    /* package */ Task<Void> removeAsync() {
        final Task<Void>.TaskCompletionSource tcs = Task.create();
        unpinInBackground(PIN_SURGES, new DeleteCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    logger.log(Level.INFO, "Unpinned successfully.");
                    tcs.setResult(null);
                } else {
                    logger.log(Level.SEVERE, "Unable to unpin Surge.", e);
                    tcs.setError(e);
                }
            }
        });
        return tcs.getTask().onSuccessTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                if (getObjectId() == null) {
                    return Task.<Void>forResult(null);
                }

                final Task<Void>.TaskCompletionSource tcs = Task.create();
                pinInBackground(PIN_DELETE, new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            logger.log(Level.INFO, "Pinned for deletion successfully.");
                            tcs.setResult(null);
                        } else {
                            logger.log(Level.SEVERE, "Unable to pin Surge for deletion.", e);
                            tcs.setError(e);
                        }
                    }
                });
                return tcs.getTask();
            }
        });
    }
}
