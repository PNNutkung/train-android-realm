package com.example.nut.realm_practice.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.nut.realm_practice.R;
import com.example.nut.realm_practice.models.Cat;
import com.example.nut.realm_practice.models.Dog;
import com.example.nut.realm_practice.models.Person;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getName();

    @BindView(R.id.container)
    LinearLayout rootLayout;

    private Realm realm;
    private RealmConfiguration realmConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        clearRootLayout();
        realmConfig();

        basicCRUD(realm);
        basicQuery(realm);
        basicLinkQuery(realm);

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String info;
                info = complexReadWrite();
                info += complexQuery();
                return info;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
            }
        }.execute();
    }

    private void realmConfig() {
        realmConfiguration = new RealmConfiguration.Builder(this).build();
        realm = Realm.getInstance(realmConfiguration);
    }

    private void clearRootLayout() {
        rootLayout.removeAllViews();
    }

    private void showStatus(String txt) {
        Log.i(TAG, txt);
        TextView tv = new TextView(this);
        tv.setText(txt);
        rootLayout.addView(tv);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
    }

    private void basicCRUD(Realm realm) {
        showStatus("Perform basic Create/Read/Update/Delete (CRUD) operations...");

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Person person = realm.createObject(Person.class);
                person.setId(1);
                person.setName("Young Person");
                person.setAge(14);
            }
        });

        // Find the first person (no query conditions) and read a field
        final Person person = realm.where(Person.class).findFirst();
        showStatus(String.format("%s : %d",person.getName(),person.getAge()));

        // Update person in a transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                person.setName("Senior Person");
                person.setAge(99);
                showStatus(String.format("%s got older: %d",person.getName(),person.getAge()));
            }
        });

        // Delete all persons
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.delete(Person.class);
            }
        });
    }

    private void basicQuery(Realm realm) {
        showStatus("\nPerforming basic Query operation...");
        showStatus(String.format("Number of persons: %d", realm.where(Person.class).count()));
        RealmResults<Person> results = realm.where(Person.class)
                .equalTo("age", 90)
                .findAll();
        showStatus("Size of result set: " + results.size());
    }

    private void basicLinkQuery(Realm realm) {
        showStatus("\nPerforming basic Query operation...");
        showStatus("Number of persons: " + realm.where(Person.class).count());

        RealmResults<Person> results = realm.where(Person.class).equalTo("cats.name", "Tiger").findAll();

        showStatus(String.format("Size of result set: %d", results.size()));
    }

    private String complexReadWrite() {
        String status = "\nPerforming complex Read/Write operation...";

        Realm realm = Realm.getInstance(realmConfiguration);

        // Add ten persons in one transaction
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Dog fido = realm.createObject(Dog.class);
                fido.name = "fido";
                for (int i = 0; i < 10; ++i) {
                    Person person = realm.createObject(Person.class);
                    person.setId(i);
                    person.setName(String.format("Person no. %d",i));
                    person.setAge(i);
                    person.setDog(fido);

                    // The field tempReference is annotated with @Ignore.
                    // This means setTempReference sets the Person tempReference
                    // field directly. The tempReference is NOT saved as part of
                    // the RealmObject:
                    person.setTempReference(42);

                    for (int j = 0; j < i; ++j) {
                        Cat cat = realm.createObject(Cat.class);
                        cat.name = String.format("Cat_%d", j);
                        person.getCats().add(cat);
                    }
                }
            }
        });

        // Implicit read transactions allow you to access your objects
        status += "\nNumber of persons: " + realm.where(Person.class).count();

        // Sorting
        RealmResults<Person> sortedPersons = realm.where(Person.class).findAllSorted("age", Sort.DESCENDING);
        status += String.format("\nSorting %s == %s",sortedPersons.last().getName(),realm.where(Person.class).findFirst());
        realm.close();
        return status;
    }

    private String complexQuery() {
        String status = String.format("\n\nPerforming complex Query operation...");

        Realm realm = Realm.getInstance(realmConfiguration);
        status += String.format("\nNumber of persons: %d", realm.where(Person.class).count());

        // Find all persons where age between 7 and 9 and name begins with "Person".
        RealmResults<Person> results = realm.where(Person.class)
                .between("age", 7, 9)
                .beginsWith("name", "Person")
                .findAll();
        status += String.format("\nSize of result set: %d", results.size());
        realm.close();
        return status;
    }
}
