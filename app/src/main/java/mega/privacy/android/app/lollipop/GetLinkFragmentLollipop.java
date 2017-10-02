package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class GetLinkFragmentLollipop extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, android.widget.CompoundButton.OnCheckedChangeListener {

    Context context;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    ActionBar aB;
    Button sendButton;
    Button copyButton;
    SwitchCompat switchButtonExpiry;
    Button expiryDateButton;

    DatePickerDialog datePickerDialog;

    private LinearLayout mainLinearLayout;
    private ScrollView scrollView;

    String link;
    boolean isExpiredDateLink;

    NodeController nC;

    CheckedTextView linkWithoutKeyCheck;
    CheckedTextView linkDecryptionKeyCheck;
    CheckedTextView linkWithKeyCheck;
    TextView subtitleProOnlyExpiry;
    TextView linkText;

    TextView subtitleProOnlyProtection;
    SwitchCompat switchButtonProtection;
    TextView passwordProtectionEditText;

    RelativeLayout transparentKeyLayoutExpiry;
    RelativeLayout transparentKeyLayoutProtection;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }

        nC = new NodeController(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if(megaApi==null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        View v = inflater.inflate(R.layout.fragment_get_link, container, false);

        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_get_link);
        mainLinearLayout = (LinearLayout) v.findViewById(R.id.get_link_main_linear_layout);
        switchButtonExpiry = (SwitchCompat) v.findViewById(R.id.switch_set_expiry_date);
        expiryDateButton = (Button) v.findViewById(R.id.expiry_date_button);

        //Set by default, link with key
        linkWithoutKeyCheck = (CheckedTextView) v.findViewById(R.id.link_without_key);
        linkDecryptionKeyCheck = (CheckedTextView) v.findViewById(R.id.link_decryption_key);
        linkWithKeyCheck = (CheckedTextView) v.findViewById(R.id.link_with_key);

        linkText = (TextView)v.findViewById(R.id.link);
        subtitleProOnlyExpiry = (TextView)v.findViewById(R.id.subtitle_set_expiry_date);

        linkWithoutKeyCheck.setOnClickListener(this);
        linkDecryptionKeyCheck.setOnClickListener(this);
        linkWithKeyCheck.setOnClickListener(this);

        sendButton = (Button) v.findViewById(R.id.send_button);
        copyButton = (Button) v.findViewById(R.id.copy_button);

        sendButton.setOnClickListener(this);
        copyButton.setOnClickListener(this);

        transparentKeyLayoutExpiry = (RelativeLayout) v.findViewById(R.id.transparent_key_layout_expiry_date);

        subtitleProOnlyProtection = (TextView)v.findViewById(R.id.subtitle_set_password_protection);
        switchButtonProtection = (SwitchCompat) v.findViewById(R.id.switch_set_password_protection);
        passwordProtectionEditText = (TextView) v.findViewById(R.id.password_protection_edit);
        passwordProtectionEditText.setOnClickListener(this);

        transparentKeyLayoutProtection = (RelativeLayout) v.findViewById(R.id.transparent_key_layout_password_protection);

        if(((GetLinkActivityLollipop)context).selectedNode.isExported()){
            log("node is already exported: "+((GetLinkActivityLollipop)context).selectedNode.getName());
            log("node link: "+((GetLinkActivityLollipop)context).selectedNode.getPublicLink());
            link = ((GetLinkActivityLollipop)context).selectedNode.getPublicLink();

            linkWithoutKeyCheck.setChecked(false);
            linkDecryptionKeyCheck.setChecked(false);
            linkWithKeyCheck.setChecked(true);
            linkText.setText(link);
        }
        else{
            nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
            linkText.setText(getString(R.string.link_request_status));
        }

		if(((GetLinkActivityLollipop)context).accountType > MegaAccountDetails.ACCOUNT_TYPE_FREE){
			log("The user is PRO - enable expiration date");

            transparentKeyLayoutExpiry.setVisibility(View.GONE);

			if(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime()<=0){
                switchButtonExpiry.setChecked(false);
				expiryDateButton.setVisibility(View.GONE);
                subtitleProOnlyExpiry.setVisibility(View.VISIBLE);
			}
			else{
				switchButtonExpiry.setChecked(true);
				java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
				Calendar cal = Util.calculateDateFromTimestamp(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime());
				TimeZone tz = cal.getTimeZone();
				df.setTimeZone(tz);
				Date date = cal.getTime();
				String formattedDate = df.format(date);
				expiryDateButton.setText(formattedDate);
				expiryDateButton.setVisibility(View.VISIBLE);
                subtitleProOnlyExpiry.setVisibility(View.GONE);
			}

            switchButtonExpiry.setEnabled(true);
            switchButtonExpiry.setOnCheckedChangeListener(this);
            expiryDateButton.setOnClickListener(this);

            //Check if the link has password
            transparentKeyLayoutProtection.setVisibility(View.GONE);

            switchButtonProtection.setChecked(false);
            passwordProtectionEditText.setVisibility(View.GONE);
            subtitleProOnlyProtection.setVisibility(View.VISIBLE);

            switchButtonProtection.setEnabled(true);
            switchButtonProtection.setOnCheckedChangeListener(this);
            switchButtonProtection.setOnClickListener(this);
		}
		else{
			log("The is user is not PRO");
            transparentKeyLayoutExpiry.setVisibility(View.VISIBLE);

            switchButtonExpiry.setEnabled(false);
			expiryDateButton.setVisibility(View.GONE);
            subtitleProOnlyExpiry.setVisibility(View.VISIBLE);

            transparentKeyLayoutProtection.setVisibility(View.VISIBLE);

            switchButtonProtection.setEnabled(false);
            passwordProtectionEditText.setVisibility(View.GONE);
            subtitleProOnlyProtection.setVisibility(View.VISIBLE);
		}

        return v;
    }

    public void showDatePicker(long expirationTimestamp){
        log("showDatePicker: "+expirationTimestamp);
        int year;
        int month;
        int day;

        if(expirationTimestamp!=-1){
            java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
            Calendar c = Util.calculateDateFromTimestamp(expirationTimestamp);
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }
        else{
            Calendar c = Calendar.getInstance();
            year = c.get(Calendar.YEAR);
            month = c.get(Calendar.MONTH);
            day = c.get(Calendar.DAY_OF_MONTH);
        }

        datePickerDialog = new DatePickerDialog(context, this, year, month, day);
        datePickerDialog.show();
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()){
            case R.id.expiry_date_button:{
                showDatePicker(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime());
                break;
            }
            case R.id.disagree_button:{
                log("DISAgree button");
                ((GetLinkActivityLollipop)context).finish();
                break;
            }
            case R.id.agree_button:{
                log("Agree button");
                break;
            }
            case R.id.link_without_key:{
                linkWithoutKeyCheck.setChecked(true);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(false);
                if(link!=null){
                    String urlString="";
                    String [] s = link.split("!");
                    if (s.length == 3){
                        urlString = s[0] + "!" + s[1];
                    }
                    linkText.setText(urlString);
                }
                break;
            }
            case R.id.link_decryption_key:{
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(true);
				linkWithKeyCheck.setChecked(false);
                if(link!=null){
                    String keyString="!";
                    String [] s = link.split("!");
                    if (s.length == 3){
                        keyString = keyString+s[2];
                    }
                    linkText.setText(keyString);
                }
                break;
            }
            case R.id.link_with_key:{
                linkWithoutKeyCheck.setChecked(false);
				linkDecryptionKeyCheck.setChecked(false);
				linkWithKeyCheck.setChecked(true);
				linkText.setText(link);
                break;
            }
            case R.id.copy_button:{
                ((GetLinkActivityLollipop)context).copyLink(linkText.getText().toString());
                break;
            }
            case R.id.send_button:{
                ((GetLinkActivityLollipop)context).sendLink(linkText.getText().toString());
                break;
            }
            case R.id.password_protection_edit:{
                ((GetLinkActivityLollipop)context).showSetPasswordDialog(null, link);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    public static void log(String message) {
        Util.log("GetLinkFragmentLollipop", message);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

        Calendar cal = Calendar.getInstance();
        cal.set(year, monthOfYear, dayOfMonth);
        Date date = cal.getTime();
        SimpleDateFormat dfTimestamp = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateString = dfTimestamp.format(date);
        dateString = dateString + "2359";
        log("the date string is: "+dateString);
        int timestamp = (int) Util.calculateTimestamp(dateString);
        log("the TIMESTAMP is: "+timestamp);
        isExpiredDateLink=true;
        nC.exportLinkTimestamp(((GetLinkActivityLollipop)context).selectedNode, timestamp);
    }

    public void enablePassProtection(boolean enable){
        switchButtonProtection.setChecked(enable);
    }

    public void processingPass(){
        linkText.setText(getString(R.string.link_request_status));
    }

    public void requestFinish(MegaRequest request, MegaError e){
        log("requestFinish");

        if (request.getType() == MegaRequest.TYPE_EXPORT) {
            log("export request finished");
            MegaNode node = ((GetLinkActivityLollipop)context).selectedNode;
            log("EXPIRATION DATE: " + node.getExpirationTime());
            if (isExpiredDateLink) {
                log("change the expiration date");
                if (node.getExpirationTime() <= 0) {
                    switchButtonExpiry.setChecked(false);
                    expiryDateButton.setVisibility(View.GONE);
                    subtitleProOnlyExpiry.setVisibility(View.VISIBLE);
                } else {
                    switchButtonExpiry.setChecked(true);
                    java.text.DateFormat df = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault());
                    Calendar cal = Util.calculateDateFromTimestamp(node.getExpirationTime());
                    TimeZone tz = cal.getTimeZone();
                    df.setTimeZone(tz);
                    Date date = cal.getTime();
                    String formattedDate = df.format(date);
                    expiryDateButton.setText(formattedDate);
                    expiryDateButton.setVisibility(View.VISIBLE);
                    subtitleProOnlyExpiry.setVisibility(View.GONE);
                }
            } else {
                link = request.getLink();

                if(linkWithoutKeyCheck.isChecked()){
                    if(link!=null){
                        String urlString="";
                        String [] s = link.split("!");
                        if (s.length == 3){
                            urlString = s[0] + "!" + s[1];
                        }
                        linkText.setText(urlString);
                    }
                }
                else if(linkDecryptionKeyCheck.isChecked()){
                    if(link!=null){
                        String keyString="!";
                        String [] s = link.split("!");
                        if (s.length == 3){
                            keyString = keyString+s[2];
                        }
                        linkText.setText(keyString);
                    }
                }
                else{
                    linkText.setText(link);
                }
            }
            log("link: " + request.getLink());

            isExpiredDateLink = false;
        }
        else if(request.getType() == MegaRequest.TYPE_PASSWORD_LINK){
            log("password link request finished");
            linkText.setText(request.getText());
            passwordProtectionEditText.setText(request.getPassword());
            passwordProtectionEditText.setVisibility(View.VISIBLE);
            subtitleProOnlyProtection.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        log("onCheckedChanged");

        switch (buttonView.getId()){
            case R.id.switch_set_expiry_date:{
                log("Set expiry date");
                if(switchButtonExpiry.isChecked()){
                    showDatePicker(-1);
                }
                else{
                    isExpiredDateLink=true;
                    nC.exportLink(((GetLinkActivityLollipop)context).selectedNode);
                }
                break;
            }
            case R.id.switch_set_password_protection:{
//                showDatePicker(((GetLinkActivityLollipop)context).selectedNode.getExpirationTime());
                log("Set password protection");
                if(switchButtonProtection.isChecked()){
                    ((GetLinkActivityLollipop)context).showSetPasswordDialog(null, link);
                }
                else{
                    log("Remove pass protection");
                    linkText.setText(link);
                    passwordProtectionEditText.setVisibility(View.GONE);
                    subtitleProOnlyProtection.setVisibility(View.VISIBLE);
                }

                break;
            }
        }
    }
}
