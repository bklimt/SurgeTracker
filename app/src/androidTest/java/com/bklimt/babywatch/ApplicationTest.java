package com.bklimt.babywatch;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.bklimt.babywatch.backbone.Collection;
import com.bklimt.babywatch.backbone.Model;

import java.util.Comparator;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testCollectionSort() {
        Comparator<Model> comparator = new Comparator<Model>() {
            @Override
            public int compare(Model lhs, Model rhs) {
                String lhsString = lhs.getString("key");
                String rhsString = rhs.getString("key");
                if (lhsString == null) {
                    return -1;
                }
                if (rhsString == null) {
                    return 1;
                }
                return lhsString.compareTo(rhsString);
            }
        };

        Collection<Model> collection = new Collection<Model>();
        collection.setComparator(comparator);

        Model model = new Model();
        model.set("key", "hello");
        model.set("order", 1);
        collection.add(model);

        model = new Model();
        model.set("key", "hello");
        model.set("order", 2);
        collection.add(model);

        model = new Model();
        collection.add(model);

        model = new Model();
        model.set("key", "world");
        collection.add(model);

        model = new Model();
        model.set("key", "apple");
        collection.add(model);

        assertEquals(5, collection.size());

        assertEquals(null, collection.get(0).get("key"));
        assertEquals("apple", collection.get(1).getString("key"));
        assertEquals("hello", collection.get(2).getString("key"));
        assertEquals(1, collection.get(2).getInt("order"));
        assertEquals("hello", collection.get(3).getString("key"));
        assertEquals(2, collection.get(3).getInt("order"));
        assertEquals("world", collection.get(4).getString("key"));
    }
}