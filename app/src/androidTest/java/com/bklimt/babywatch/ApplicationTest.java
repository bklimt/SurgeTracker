package com.bklimt.babywatch;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.bklimt.babywatch.backbone.Collection;
import com.bklimt.babywatch.backbone.Model;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testCollectionSort() {
        Collection<Model> collection = new Collection<Model>();

        Model model = new Model();
        model.set("key", "hello");
        collection.add(model);

        model = new Model();
        model.set("key", "world");
        collection.add(model);

        model = new Model();
        model.set("key", "apple");
        collection.add(model);

        assertEquals(3, collection.size());

        assertEquals("apple", collection.get(0).get("key"));
        assertEquals("hello", collection.get(1).get("key"));
        assertEquals("world", collection.get(2).get("key"));
    }
}