package ca.uwaterloo.crysp.privacyguard.Application.Activities;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import ca.uwaterloo.crysp.privacyguard.Application.Database.CryptominerAlert;
import ca.uwaterloo.crysp.privacyguard.Application.Database.DatabaseHandler;
import ca.uwaterloo.crysp.privacyguard.Application.PrivacyGuard;
import ca.uwaterloo.crysp.privacyguard.R;

public class CryptoDetailActivity extends AppCompatActivity {

    private int notifyId;
    private String packageName;
    private String appName;
    private String category;
    private int ignore;

    private ListView list;
    private DetailCryptoListViewAdapter adapter;
    private Switch notificationSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_detail);

        // Get the message from the intent
        Intent intent = getIntent();
        notifyId = intent.getIntExtra(PrivacyGuard.EXTRA_ID, -1);
        packageName = intent.getStringExtra(PrivacyGuard.EXTRA_PACKAGE_NAME);
        appName = intent.getStringExtra(PrivacyGuard.EXTRA_APP_NAME);
        category = intent.getStringExtra(PrivacyGuard.EXTRA_CATEGORY);
        ignore = intent.getIntExtra(PrivacyGuard.EXTRA_IGNORE, 0);

        TextView title = (TextView) findViewById(R.id.detail_title);
        title.setText(category);
        TextView subtitle = (TextView) findViewById(R.id.detail_subtitle);
        subtitle.setText("[" + appName + "]");


        notificationSwitch = (Switch) findViewById(R.id.detail_switch);
        notificationSwitch.setChecked(ignore == 1);
        notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DatabaseHandler db = DatabaseHandler.getInstance(CryptoDetailActivity.this);
                if (isChecked) {
                    // The toggle is enabled
                    db.setIgnoreAppCategory(notifyId, true);
                    ignore = 1;
                } else {
                    // The toggle is disabled
                    db.setIgnoreAppCategory(notifyId, false);
                    ignore = 0;
                }
            }
        });


        list = (ListView) findViewById(R.id.detail_list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CryptominerAlert alert = (CryptominerAlert) parent.getItemAtPosition(position);
                Intent intent;
                intent = new Intent(CryptoDetailActivity.this, PacketDetailActivity.class);

                intent.putExtra(PrivacyGuard.EXTRA_REF_PACKETID, alert.getRefPacketId());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateList();
    }

    private void updateList() {
        DatabaseHandler db = DatabaseHandler.getInstance(this);
        List<CryptominerAlert>  alerts = db.getAppCryptoAlerts(packageName);

        if (alerts == null) {
            return;
        }

        if (adapter == null) {
            adapter = new DetailCryptoListViewAdapter(this, alerts);

            View header = getLayoutInflater().inflate(R.layout.listview_detail_crypto, null);
            ((TextView) header.findViewById(R.id.detail_domain)).setText(R.string.domain_label);
            ((TextView) header.findViewById(R.id.detail_time)).setText(R.string.time_label);
            ((TextView) header.findViewById(R.id.detail_signature)).setText(R.string.signature_label);
            ((TextView) header.findViewById(R.id.detail_ispool)).setText(R.string.ispool_label);

            list.addHeaderView(header);
            list.setAdapter(adapter);
        } else {
            adapter.updateData(alerts);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = getParentActivityIntent();
                if (shouldUpRecreateTask(upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                                    // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    navigateUpTo(upIntent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
