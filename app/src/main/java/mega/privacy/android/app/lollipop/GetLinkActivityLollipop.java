package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;


public class GetLinkActivityLollipop extends PinActivityLollipop implements MegaRequestListenerInterface {

	float scaleH, scaleW;
	float density;
	DisplayMetrics outMetrics;
	Display display;

	CoordinatorLayout fragmentContainer;

	private android.support.v7.app.AlertDialog passwordDialog;

	//Fragments
	GetLinkFragmentLollipop getLinkFragment;
	CopyrightFragmentLollipop copyrightFragment;

	ActionBar aB;
	Toolbar tB;

	public int visibleFragment= Constants.COPYRIGHT_FRAGMENT;

	static GetLinkActivityLollipop getLinkActivity;

	Intent intentReceived = null;

	public long handle;
	public MegaNode selectedNode;
	public int accountType;

	DatabaseHandler dbH;

    Handler handler = new Handler();
	private MegaApiAndroid megaApi;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);

		getLinkActivity = this;

		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		density  = getResources().getDisplayMetrics().density;

		scaleW = Util.getScaleW(outMetrics, density);
		scaleH = Util.getScaleH(outMetrics, density);

	    dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		if (megaApi == null){
			megaApi = ((MegaApplication) getApplication()).getMegaApi();
		}

		intentReceived = getIntent();
		if (intentReceived != null){
			handle = intentReceived.getLongExtra("handle", -1);
			if(handle!=-1){
				selectedNode = megaApi.getNodeByHandle(handle);
			}
			else{
				finish();
			}
			accountType = intentReceived.getIntExtra("account", 0);
		}
		else{
			finish();
		}

		setContentView(R.layout.get_link_activity_layout);

		fragmentContainer = (CoordinatorLayout) findViewById(R.id.get_link_coordinator_layout);
		tB = (Toolbar) findViewById(R.id.toolbar_get_link);
		if(tB==null){
			log("Tb is Null");
		}

		tB.setVisibility(View.GONE);
		setSupportActionBar(tB);
		aB = getSupportActionBar();
		aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
		aB.setHomeButtonEnabled(true);
		aB.setDisplayHomeAsUpEnabled(true);

		if(selectedNode.isExported()){

			visibleFragment = Constants.GET_LINK_FRAGMENT;
		}
		else{

			visibleFragment = Constants.COPYRIGHT_FRAGMENT;
		}
		showFragment(visibleFragment);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

	public void sendLink(String link){
		log("sendLink");
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, link);
		startActivity(Intent.createChooser(intent, getString(R.string.context_get_link)));
	}

	public void copyLink(String link){
		log("copyLink");
		if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(link);
		} else {
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", link);
			clipboard.setPrimaryClip(clip);
		}
		showSnackbar(getString(R.string.file_properties_get_link));
	}

	public void showSnackbar(String message){
		Snackbar snackbar = Snackbar.make(fragmentContainer,message,Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}


	public void showSetPasswordDialog(final String password, final String link){
		log("showSetPasswordDialog");

		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.overquota_alert_title));
		LayoutInflater inflater = getLayoutInflater();
		View dialoglayout = inflater.inflate(R.layout.dialog_set_password_link, null);
		builder.setTitle(getString(R.string.set_password_protection_dialog));

		final EditText input1 = (EditText) dialoglayout.findViewById(R.id.first_edit_text);
		final EditText input2 = (EditText) dialoglayout.findViewById(R.id.second_edit_text);

		if(password!=null){
			input1.setText(password);
		}

		input1.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					showKeyboardDelayed(v);
				}
			}
		});

		final RelativeLayout errorLayout1 = (RelativeLayout) dialoglayout.findViewById(R.id.error_first_edit_layout);
		final RelativeLayout errorLayout2 = (RelativeLayout) dialoglayout.findViewById(R.id.error_second_edit_layout);

		final TextView textError1 = (TextView) dialoglayout.findViewById(R.id.error_first_edit_text);
		final TextView textError2 = (TextView) dialoglayout.findViewById(R.id.error_second_edit_text);

		builder.setView(dialoglayout);


		builder.setPositiveButton(getString(R.string.button_set), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});

		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input1.getBackground().clearColorFilter();
				if(getLinkFragment!=null){
					getLinkFragment.enablePassProtection(false);
				}
			}
		});

		input1.getBackground().mutate().clearColorFilter();
		input1.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input1.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(errorLayout1.getVisibility() == View.VISIBLE){
					errorLayout1.setVisibility(View.GONE);
					input1.getBackground().mutate().clearColorFilter();
					input1.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input2.getBackground().mutate().clearColorFilter();
		input2.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input2.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(errorLayout2.getVisibility() == View.VISIBLE){
					errorLayout2.setVisibility(View.GONE);
					input2.getBackground().mutate().clearColorFilter();
					input2.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_NEXT) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input1.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						errorLayout1.setVisibility(View.VISIBLE);
						input1.requestFocus();
						return true;
					}
					else{
						input2.requestFocus();
					}
					return true;
				}
				return false;
			}
		});

		passwordDialog = builder.create();
		passwordDialog.show();
		passwordDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				boolean proceedInput1 = false;
				boolean proceedInput2 = false;
				String value1 = input1.getText().toString();
				if (value1.trim().length() != 0) {
					proceedInput1 = true;
				}

				String value2 = input2.getText().toString();
				if (value2.trim().length() != 0) {
					proceedInput2 = true;
				}

				if(proceedInput1&&proceedInput2){
					log("Check are equal");
					if(value1.equals(value2)){
						log("Proceed to set pass");
						getLinkFragment.processingPass();
						megaApi.encryptLinkWithPassword(link, value2, getLinkActivity);
						passwordDialog.dismiss();
					}
					else{
						input2.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						errorLayout2.setVisibility(View.VISIBLE);
						textError2.setText(getString(R.string.error_passwords_dont_match));
						input2.requestFocus();
					}
				}
				else if(!proceedInput1&&proceedInput2){
					log("Error on pass1");
					input1.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					errorLayout1.setVisibility(View.VISIBLE);
					input1.requestFocus();
				}
				else if(!proceedInput2&&proceedInput1){
					log("Error on pass2");
					input2.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					errorLayout2.setVisibility(View.VISIBLE);
					input2.requestFocus();
				}
				else{
					log("Error on both");
					input1.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					errorLayout1.setVisibility(View.VISIBLE);
					input1.requestFocus();

					input2.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					errorLayout2.setVisibility(View.VISIBLE);
				}
			}
		});
	}

	private void showKeyboardDelayed(final View view) {
		log("showKeyboardDelayed");
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}

	public void showFragment(int visibleFragment){
		log("showFragment: "+visibleFragment);
		this.visibleFragment = visibleFragment;
		switch (visibleFragment){
			case Constants.GET_LINK_FRAGMENT:{
				log("show GET_LINK_FRAGMENT");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
				}

				if(aB!=null){
					if(selectedNode.isExported()){

						aB.setTitle(R.string.edit_link_option);
					}
					else{

						aB.setTitle(R.string.context_get_link_menu);
					}
					aB.show();
				}

				if(getLinkFragment==null){
					getLinkFragment = new GetLinkFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_get_link, getLinkFragment);
				ft.commitNowAllowingStateLoss();

				break;
			}
			case Constants.COPYRIGHT_FRAGMENT:{
				log("Show COPYRIGHT_FRAGMENT");

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Window window = this.getWindow();
					window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
					window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
					window.setStatusBarColor(ContextCompat.getColor(this, R.color.transparent_black));
				}

				if(aB!=null){
					aB.hide();
				}

				if(copyrightFragment==null){
					copyrightFragment = new CopyrightFragmentLollipop();
				}

				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.replace(R.id.fragment_container_get_link, copyrightFragment);
				ft.commitNowAllowingStateLoss();
				break;
			}
		}
	}

	@Override
	public void onResume() {
		log("onResume");
		super.onResume();
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

	}

	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
		log("onRequestFinish");

		if (e.getErrorCode() == MegaError.API_OK) {
			log("link: " + request.getLink());
			selectedNode = megaApi.getNodeByHandle(request.getNodeHandle());
			if(getLinkFragment!=null){
				if(getLinkFragment.isAdded()){
					getLinkFragment.requestFinish(request, e);
				}
			}
		} else {
			log("Error: " + e.getErrorString());
			showSnackbar(getString(R.string.context_no_link));
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

	}

	public static void log(String message) {
		Util.log("GetLinkActivityLollipop", message);
	}

}
