package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.components.ExtendedViewPager;
import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.lollipop.adapters.MegaFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaOfflineFullScreenImageAdapterLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

public class FullScreenImageViewerLollipop extends PinActivityLollipop implements OnPageChangeListener, OnClickListener, MegaRequestListenerInterface, OnItemClickListener{
	
	private DisplayMetrics outMetrics;

	private boolean aBshown = true;
	
	ProgressDialog statusDialog;

	int accountType;

	private boolean isGetLink = false;
	int positionToRemove = -1;

	float scaleText;
	
	private MegaFullScreenImageAdapterLollipop adapterMega;
	private MegaOfflineFullScreenImageAdapterLollipop adapterOffline;
	private int positionG;
	private ArrayList<Long> imageHandles;
	private boolean fromShared = false;
	private RelativeLayout fragmentContainer;
	private TextView fileNameTextView;
	private ImageView actionBarIcon;
	private ImageView overflowIcon;
	private ImageView shareIcon;
	private ImageView downloadIcon;
	private ImageView propertiesIcon;
	private ImageView linkIcon;
	private ListView overflowMenuList;
	private boolean overflowVisible = false; 
	
	private RelativeLayout bottomLayout;
    private RelativeLayout topLayout;
	private ExtendedViewPager viewPager;	
	
	static FullScreenImageViewerLollipop fullScreenImageViewer;
    private MegaApiAndroid megaApi;

    private ArrayList<String> paths;
    
    int adapterType = 0;
    
    public static int REQUEST_CODE_SELECT_MOVE_FOLDER = 1001;
	public static int REQUEST_CODE_SELECT_COPY_FOLDER = 1002;
	public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;
	
	MegaNode node;
	
	boolean shareIt = true;
	boolean moveToRubbish = false;
	
	private static int EDIT_TEXT_ID = 1;
	private Handler handler;
	
	private AlertDialog renameDialog;
	
	int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
	
	DatabaseHandler dbH = null;
	MegaPreferences prefs = null;
	
	boolean isFolderLink = false;
	
	ArrayList<Long> handleListM = new ArrayList<Long>();
	
	ArrayList<MegaOffline> mOffList;
	ArrayList<MegaOffline> mOffListImages;
	
	@Override
	public void onDestroy(){
		if(megaApi != null)
		{	
			megaApi.removeRequestListener(this);
		}
		
		super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_MENU ) {
	        // do nothing
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		super.onCreate(savedInstanceState);
		
		handler = new Handler();
		fullScreenImageViewer = this;

		Display display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
		display.getMetrics(outMetrics);
		float density  = getResources().getDisplayMetrics().density;

		float scaleW = Util.getScaleW(outMetrics, density);
		float scaleH = Util.getScaleH(outMetrics, density);
		if (scaleH < scaleW){
			scaleText = scaleH;
		}
		else{
			scaleText = scaleW;
		}
		
		dbH = DatabaseHandler.getDbHandler(this);
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
		    requestWindowFeature(Window.FEATURE_NO_TITLE); 
		    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		
		setContentView(R.layout.activity_full_screen_image_viewer);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
		    ActionBar actionBar = getSupportActionBar();
		    if (actionBar != null){
		    	actionBar.hide();
		    }
		}
		
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
	        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }
		
		display = getWindowManager().getDefaultDisplay();
		outMetrics = new DisplayMetrics ();
	    display.getMetrics(outMetrics);
	    density  = getResources().getDisplayMetrics().density;
		
	    scaleW = Util.getScaleW(outMetrics, density);
	    scaleH = Util.getScaleH(outMetrics, density);
		
		viewPager = (ExtendedViewPager) findViewById(R.id.image_viewer_pager);
		viewPager.setPageMargin(40);
		
		fragmentContainer = (RelativeLayout) findViewById(R.id.full_image_viewer_parent_layout);
		
		Intent intent = getIntent();
		positionG = intent.getIntExtra("position", 0);
		orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
		isFolderLink = intent.getBooleanExtra("isFolderLink", false);

		accountType = intent.getIntExtra("typeAccount", MegaAccountDetails.ACCOUNT_TYPE_FREE);

		MegaApplication app = (MegaApplication)getApplication();
		if (isFolderLink){
			megaApi = app.getMegaApiFolder();
		}
		else{
			megaApi = app.getMegaApi();
		}
		
		imageHandles = new ArrayList<Long>();
		paths = new ArrayList<String>();
		long parentNodeHandle = intent.getLongExtra("parentNodeHandle", -1);
		fromShared = intent.getBooleanExtra("fromShared", false);
		MegaNode parentNode;		
		
		adapterType = intent.getIntExtra("adapterType", 0);
		if (adapterType == Constants.OFFLINE_ADAPTER){
			mOffList = new ArrayList<MegaOffline>();
			
			String pathNavigation = intent.getStringExtra("pathNavigation");
			int orderGetChildren = intent.getIntExtra("orderGetChildren", MegaApiJava.ORDER_DEFAULT_ASC);
			log("PATHNAVIGATION: " + pathNavigation);
			mOffList=dbH.findByPath(pathNavigation);
			log ("mOffList.size() = " + mOffList.size());
			
			for(int i=0; i<mOffList.size();i++){
				MegaOffline checkOffline = mOffList.get(i);
				
				if(!checkOffline.isIncoming()){				
					log("NOT isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + checkOffline.getPath()+checkOffline.getName());
					}
					else{
						offlineDirectory = getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove A: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);		
						i--;
					}	
				}
				else{
					log("isIncomingOffline");
					File offlineDirectory = null;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" +checkOffline.getHandleIncoming() + "/" + checkOffline.getPath()+checkOffline.getName());
						log("offlineDirectory: "+offlineDirectory);
					}
					else{
						offlineDirectory = getFilesDir();
					}	
					
					if (!offlineDirectory.exists()){
						log("Path to remove B: "+(mOffList.get(i).getPath()+mOffList.get(i).getName()));
						//dbH.removeById(mOffList.get(i).getId());
						mOffList.remove(i);
						i--;
					}						
				}
			}
			
			if (mOffList != null){
				if(!mOffList.isEmpty()) {
					MegaOffline lastItem = mOffList.get(mOffList.size()-1);
					if(!(lastItem.getHandle().equals("0"))){
						String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
						log("Export in: "+path);
						File file= new File(path);
						if(file.exists()){
							MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", false, "0");
							mOffList.add(masterKeyFile);
						}
					}	
				}
				else{
					String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
					log("Export in: "+path);
					File file= new File(path);
					if(file.exists()){
						MegaOffline masterKeyFile = new MegaOffline("0", path, "MEGARecoveryKey.txt", 0, "0", false, "0");
						mOffList.add(masterKeyFile);
					}
				}
			}
			
			if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
				sortByNameDescending();
			}
			else{
				sortByNameAscending();
			}
			
			if (mOffList.size() > 0){
				
				mOffListImages = new ArrayList<MegaOffline>();
				int positionImage = -1;
				for (int i=0;i<mOffList.size();i++){
					if (MimeTypeList.typeForName(mOffList.get(i).getName()).isImage()){
						mOffListImages.add(mOffList.get(i));
						positionImage++;
						if (i == positionG){
							positionG = positionImage;
						}
					}
				}
				
				if (positionG >= mOffListImages.size()){
					positionG = 0;
				}
				
				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(fullScreenImageViewer, mOffListImages);
				viewPager.setAdapter(adapterOffline);
				
				viewPager.setCurrentItem(positionG);
		
				viewPager.setOnPageChangeListener(this);
				
				actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
				actionBarIcon.setOnClickListener(this);
				
				overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
				overflowIcon.setVisibility(View.INVISIBLE);
				
				downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
				downloadIcon.setVisibility(View.GONE);
				
				propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
				propertiesIcon.setVisibility(View.GONE);
				
				linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
				linkIcon.setVisibility(View.GONE);
				
				shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
				shareIcon.setVisibility(View.GONE);
				
				bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
			    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
			}

			fileNameTextView = (TextView) findViewById(R.id.full_image_viewer_file_name);
			fileNameTextView.setText(mOffListImages.get(positionG).getName());
		}				
		else if (adapterType == Constants.ZIP_ADAPTER){
			String offlinePathDirectory = intent.getStringExtra("offlinePathDirectory");
		
			File offlineDirectory = new File(offlinePathDirectory);
//			if (Environment.getExternalStorageDirectory() != null){
//				offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
//			}
//			else{
//				offlineDirectory = getFilesDir();
//			}
			
			paths.clear();			
			int imageNumber = 0;
			int index = 0;
			File[] fList = offlineDirectory.listFiles();
			if(fList == null)
			{
				//Nothing to show (folder deleted?)
				//Close the image viewer
				finish();
				return;
			}
			
			log("SIZE: " + fList.length);
			for (File f : fList){
				log("F: " + f.getAbsolutePath());
				if (MimeTypeList.typeForName(f.getName()).isImage()){
					paths.add(f.getAbsolutePath());
					if (index == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
				index++;				
			}
			
			if(paths.size() == 0)
			{
				//No images to show (images deleted?)
				//Close the image viewer
				finish();
				return;
			}
			
			if(positionG >= paths.size())
			{
				//Invalid index. Show the first image
				positionG = 0;
			}
			
			if(adapterType == Constants.ZIP_ADAPTER){
				adapterOffline = new MegaOfflineFullScreenImageAdapterLollipop(fullScreenImageViewer, paths, true);
			}
			
			viewPager.setAdapter(adapterOffline);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			overflowIcon.setVisibility(View.INVISIBLE);
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.GONE);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			propertiesIcon.setVisibility(View.GONE);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			linkIcon.setVisibility(View.GONE);
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setVisibility(View.GONE);
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		}
		else if(adapterType == Constants.SEARCH_ADAPTER){
			ArrayList<MegaNode> nodes = null;
			if (parentNodeHandle == -1){
				String query = intent.getStringExtra("searchQuery");
				nodes = megaApi.search(query);
			}
			else{
				parentNode =  megaApi.getNodeByHandle(parentNodeHandle);
				nodes = megaApi.getChildren(parentNode, orderGetChildren);
			}
			
			int imageNumber = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (MimeTypeList.typeForName(n.getName()).isImage()){
					imageHandles.add(n.getHandle());
					if (i == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
			}

			if(imageHandles.size() == 0)
			{
				finish();
				return;
			}
			
			if(positionG >= imageHandles.size())
			{
				positionG = 0;
			}
			
			adapterMega = new MegaFullScreenImageAdapterLollipop(fullScreenImageViewer,imageHandles, megaApi);
			
			viewPager.setAdapter(adapterMega);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			if (!isFolderLink){
				overflowIcon.setVisibility(View.VISIBLE);
				overflowIcon.setOnClickListener(this);
			}
			else{
				overflowIcon.setVisibility(View.INVISIBLE);
			}
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);
			shareIcon.setVisibility(View.VISIBLE);
			shareIcon.setOnClickListener(this);
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.VISIBLE);
			downloadIcon.setOnClickListener(this);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			propertiesIcon.setVisibility(View.VISIBLE);
			propertiesIcon.setOnClickListener(this);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			linkIcon.setVisibility(View.VISIBLE);
			linkIcon.setOnClickListener(this);
			
			String menuOptions[] = new String[4];
//			menuOptions[0] = getString(R.string.context_get_link_menu);
			menuOptions[0] = getString(R.string.context_rename);
			menuOptions[1] = getString(R.string.context_move);
			menuOptions[2] = getString(R.string.context_copy);
			menuOptions[3] = getString(R.string.context_remove);
			
			overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
			overflowMenuList.setAdapter(arrayAdapter);
			overflowMenuList.setOnItemClickListener(this);
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);	
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		    
		    fileNameTextView = (TextView) findViewById(R.id.full_image_viewer_file_name);
		    fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
		}
		else{

			if (parentNodeHandle == -1){
	
				switch(adapterType){
					case Constants.FILE_BROWSER_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					case Constants.RUBBISH_BIN_ADAPTER:{
						parentNode = megaApi.getRubbishNode();
						break;
					}
					case Constants.SHARED_WITH_ME_ADAPTER:{
						parentNode = megaApi.getInboxNode();
						break;
					}
					case Constants.FOLDER_LINK_ADAPTER:{
						parentNode = megaApi.getRootNode();
						break;
					}
					default:{
						parentNode = megaApi.getRootNode();
						break;
					}
				} 
				
			}
			else{
				parentNode = megaApi.getNodeByHandle(parentNodeHandle);
			}
			
			ArrayList<MegaNode> nodes = megaApi.getChildren(parentNode, orderGetChildren);
			if (fromShared){
				if(orderGetChildren == MegaApiJava.ORDER_DEFAULT_DESC){
					nodes = sortByNameDescending(nodes);
				}
				else{
					nodes = sortByNameAscending(nodes);
				}
			}
			int imageNumber = 0;
			for (int i=0;i<nodes.size();i++){
				MegaNode n = nodes.get(i);
				if (MimeTypeList.typeForName(n.getName()).isImage()){
					imageHandles.add(n.getHandle());
					if (i == positionG){
						positionG = imageNumber; 
					}
					imageNumber++;
				}
			}
//			Toast.makeText(this, ""+parentNode.getName() + "_" + imageHandles.size(), Toast.LENGTH_LONG).show();
				
			if(imageHandles.size() == 0)
			{
				finish();
				return;
			}
			
			if(positionG >= imageHandles.size())
			{
				positionG = 0;
			}
			
			adapterMega = new MegaFullScreenImageAdapterLollipop(fullScreenImageViewer,imageHandles, megaApi);
			
			viewPager.setAdapter(adapterMega);
			
			viewPager.setCurrentItem(positionG);
	
			viewPager.setOnPageChangeListener(this);
			
			actionBarIcon = (ImageView) findViewById(R.id.full_image_viewer_icon);
			actionBarIcon.setOnClickListener(this);
			
			overflowIcon = (ImageView) findViewById(R.id.full_image_viewer_overflow);
			if (!isFolderLink){
				overflowIcon.setVisibility(View.VISIBLE);
				overflowIcon.setOnClickListener(this);
			}
			else{
				overflowIcon.setVisibility(View.INVISIBLE);
			}
			
			shareIcon = (ImageView) findViewById(R.id.full_image_viewer_share);

			if(adapterType==Constants.CONTACT_FILE_ADAPTER){
				shareIcon.setVisibility(View.GONE);
			}
			else{
				if(fromShared){
					shareIcon.setVisibility(View.GONE);
				}
				else{
					if (!isFolderLink){
						shareIcon.setVisibility(View.VISIBLE);
						shareIcon.setOnClickListener(this);
					}
					else{
						shareIcon.setVisibility(View.GONE);
					}
				}
			}
			
			downloadIcon = (ImageView) findViewById(R.id.full_image_viewer_download);
			downloadIcon.setVisibility(View.VISIBLE);
			downloadIcon.setOnClickListener(this);
			
			propertiesIcon = (ImageView) findViewById(R.id.full_image_viewer_properties);
			if (!isFolderLink){
				propertiesIcon.setVisibility(View.VISIBLE);
			}
			else{
				propertiesIcon.setVisibility(View.GONE);
			}
			propertiesIcon.setOnClickListener(this);
			
			linkIcon = (ImageView) findViewById(R.id.full_image_viewer_get_link);
			if (!isFolderLink){
				linkIcon.setVisibility(View.VISIBLE);
				linkIcon.setOnClickListener(this);
				if(adapterType==Constants.CONTACT_FILE_ADAPTER){
					linkIcon.setVisibility(View.GONE);
				}
				else{
					if(fromShared){
						linkIcon.setVisibility(View.GONE);
					}
					else{
						shareIcon.setVisibility(View.VISIBLE);
						shareIcon.setOnClickListener(this);
					}
				}
			}
			else{
				linkIcon.setVisibility(View.GONE);
			}

			ArrayAdapter<String> arrayAdapter;

			if(fromShared){
				node = megaApi.getNodeByHandle(imageHandles.get(positionG));

				int accessLevel = megaApi.getAccess(node);

				if(accessLevel== MegaShare.ACCESS_FULL){
					String menuOptions[] = new String[4];
					menuOptions[0] = getString(R.string.context_rename);
					menuOptions[1] = getString(R.string.context_move);
					menuOptions[2] = getString(R.string.context_copy);
					menuOptions[3] = getString(R.string.context_remove);
					overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
					arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
					overflowMenuList.setAdapter(arrayAdapter);
				}
				else{
					String menuOptions[] = new String[1];
					menuOptions[0] = getString(R.string.context_copy);
					overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
					arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
					overflowMenuList.setAdapter(arrayAdapter);
				}
			}
			else{
				String menuOptions[] = new String[4];
				menuOptions[0] = getString(R.string.context_rename);
				menuOptions[1] = getString(R.string.context_move);
				menuOptions[2] = getString(R.string.context_copy);
				menuOptions[3] = getString(R.string.context_move_to_trash);
				overflowMenuList = (ListView) findViewById(R.id.image_viewer_overflow_menu_list);
				arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, menuOptions);
				overflowMenuList.setAdapter(arrayAdapter);
			}

			overflowMenuList.setOnItemClickListener(this);
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);	
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
			
			bottomLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_bottom);
		    topLayout = (RelativeLayout) findViewById(R.id.image_viewer_layout_top);
		    
		    fileNameTextView = (TextView) findViewById(R.id.full_image_viewer_file_name);
		    fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());

			((MegaApplication) getApplication()).sendSignalPresenceActivity();
		}
	}
	
	public void sortByNameDescending(){
		
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
		
		
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}
		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(foldersOrder);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.reverse(filesOrder);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));					
				}				
			}
			
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);
	}

	
	public void sortByNameAscending(){
		log("sortByNameAscending");
		ArrayList<String> foldersOrder = new ArrayList<String>();
		ArrayList<String> filesOrder = new ArrayList<String>();
		ArrayList<MegaOffline> tempOffline = new ArrayList<MegaOffline>();
				
		for(int k = 0; k < mOffList.size() ; k++) {
			MegaOffline node = mOffList.get(k);
			if(node.getType().equals("1")){
				foldersOrder.add(node.getName());
			}
			else{
				filesOrder.add(node.getName());
			}
		}		
	
		Collections.sort(foldersOrder, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(filesOrder, String.CASE_INSENSITIVE_ORDER);

		for(int k = 0; k < foldersOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = foldersOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}			
		}
		
		for(int k = 0; k < filesOrder.size() ; k++) {
			for(int j = 0; j < mOffList.size() ; j++) {
				String name = filesOrder.get(k);
				String nameOffline = mOffList.get(j).getName();
				if(name.equals(nameOffline)){
					tempOffline.add(mOffList.get(j));
				}				
			}
			
		}
		
		mOffList.clear();
		mOffList.addAll(tempOffline);
	}
	
	public ArrayList<MegaNode> sortByNameAscending(ArrayList<MegaNode> nodes){
		log("sortByNameAscending");
		
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
		
		for (int i=0;i<nodes.size();i++){
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) > 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
		
		return nodes;
	}
	
	public ArrayList<MegaNode> sortByNameDescending(ArrayList<MegaNode> nodes){
		
		ArrayList<MegaNode> folderNodes = new ArrayList<MegaNode>();
		ArrayList<MegaNode> fileNodes = new ArrayList<MegaNode>();
		
		for (int i=0;i<nodes.size();i++){
			if (nodes.get(i).isFolder()){
				folderNodes.add(nodes.get(i));
			}
			else{
				fileNodes.add(nodes.get(i));
			}
		}
		
		for (int i=0;i<folderNodes.size();i++){
			for (int j=0;j<folderNodes.size()-1;j++){
				if (folderNodes.get(j).getName().compareTo(folderNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = folderNodes.get(j);
					MegaNode nAuxJ_1 = folderNodes.get(j+1);
					folderNodes.remove(j+1);
					folderNodes.remove(j);
					folderNodes.add(j, nAuxJ_1);
					folderNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		for (int i=0;i<fileNodes.size();i++){
			for (int j=0;j<fileNodes.size()-1;j++){
				if (fileNodes.get(j).getName().compareTo(fileNodes.get(j+1).getName()) < 0){
					MegaNode nAuxJ = fileNodes.get(j);
					MegaNode nAuxJ_1 = fileNodes.get(j+1);
					fileNodes.remove(j+1);
					fileNodes.remove(j);
					fileNodes.add(j, nAuxJ_1);
					fileNodes.add(j+1, nAuxJ);
				}
			}
		}
		
		nodes.clear();
		nodes.addAll(folderNodes);
		nodes.addAll(fileNodes);
		
		return nodes;
	}

	@Override
	public void onPageSelected(int position) {
		return;
	}
	
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		return;
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {

		if (state == ViewPager.SCROLL_STATE_IDLE){
			if (viewPager.getCurrentItem() != positionG){
				int oldPosition = positionG;
				int newPosition = viewPager.getCurrentItem();
				positionG = newPosition;
				
				try{
					if ((adapterType == Constants.OFFLINE_ADAPTER)){
						fileNameTextView.setText(mOffListImages.get(positionG).getName());
					}
					else if(adapterType == Constants.ZIP_ADAPTER){

					}
					else{
						TouchImageView tIV = adapterMega.getVisibleImage(oldPosition);
						if (tIV != null){
							tIV.setZoom(1);
						}
						fileNameTextView.setText(megaApi.getNodeByHandle(imageHandles.get(positionG)).getName());
					}
				}
				catch(Exception e){}
//				title.setText(names.get(positionG));
			}
		}
	}

	@Override
	public void onClick(View v) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		if (adapterType == Constants.OFFLINE_ADAPTER){
			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_share:{
					String offlineDirectory;
					if (Environment.getExternalStorageDirectory() != null){
						offlineDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
					}
					else{
						offlineDirectory = getFilesDir().getPath();
					}	
					
					String fileName = offlineDirectory + mOffListImages.get(positionG).getPath() + mOffListImages.get(positionG).getName();
					File previewFile = new File(fileName);
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, fileName + ": "  + getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
			}
		}
		else if (adapterType == Constants.ZIP_ADAPTER){

			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_share:{
					
					String fileName = paths.get(positionG);
					File previewFile = new File(fileName);
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
			}
		}
		else{
			node = megaApi.getNodeByHandle(imageHandles.get(positionG));
			switch (v.getId()){
				case R.id.full_image_viewer_icon:{
					finish();
					break;
				}
				case R.id.full_image_viewer_get_link:{
					shareIt = false;
			    	showGetLinkActivity(node.getHandle());
					break;
				}
				case R.id.full_image_viewer_properties:{
					Intent i = new Intent(this, FileInfoActivityLollipop.class);
					i.putExtra("handle", node.getHandle());
					i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
					i.putExtra("name", node.getName());
					startActivity(i);
					break;
				}
				case R.id.full_image_viewer_overflow:{
					if (adapterMega.isaBshown()){
						overflowVisible = adapterMega.isMenuVisible();
						if (overflowVisible){
							overflowMenuList.setVisibility(View.GONE);
							overflowVisible = false;
						}
						else{
							overflowMenuList.setVisibility(View.VISIBLE);
							overflowVisible = true;
						}
						adapterMega.setMenuVisible(overflowVisible);
					}
					break;
				}
				case R.id.full_image_viewer_share:{
					
					overflowMenuList.setVisibility(View.GONE);
					overflowVisible = false;
					adapterMega.setMenuVisible(overflowVisible);
									
					File previewFolder = PreviewUtils.getPreviewFolder(this);
					File previewFile = new File(previewFolder, node.getBase64Handle() + ".jpg");
					
					if (previewFile.exists()){
						Intent share = new Intent(android.content.Intent.ACTION_SEND);
						share.setType("image/*");
						share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + previewFile));
						startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
					}
					else{
						Snackbar.make(fragmentContainer, getString(R.string.full_image_viewer_not_preview), Snackbar.LENGTH_LONG).show();
					}
					
					break;
				}
				case R.id.full_image_viewer_download:{
					
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
						boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
						if (!hasStoragePermission) {
							ActivityCompat.requestPermissions(this,
					                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
									Constants.REQUEST_WRITE_STORAGE);
							
							handleListM.add(node.getHandle());
							
							return;
						}
					}
					
					overflowMenuList.setVisibility(View.GONE);
					overflowVisible = false;
					adapterMega.setMenuVisible(overflowVisible);
					
					ArrayList<Long> handleList = new ArrayList<Long>();
					handleList.add(node.getHandle());
					downloadNode(handleList);
					break;
				}
			}
		}
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
        	case Constants.REQUEST_WRITE_STORAGE:{
		        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
				if (hasStoragePermission) {
					downloadNode(handleListM);
				}
	        	break;
	        }
        }
    }
	
	@SuppressLint("NewApi") 
	public void downloadNode(ArrayList<Long> handleList){
		
		long size = 0;
		long[] hashes = new long[handleList.size()];
		for (int i=0;i<handleList.size();i++){
			hashes[i] = handleList.get(i);
			size += megaApi.getNodeByHandle(hashes[i]).getSize();
		}
		
		if (dbH == null){
//			dbH = new DatabaseHandler(getApplicationContext());
			dbH = DatabaseHandler.getDbHandler(getApplicationContext());
		}
		
		boolean askMe = true;
		String downloadLocationDefaultPath = "";
		prefs = dbH.getPreferences();		
		if (prefs != null){
			if (prefs.getStorageAskAlways() != null){
				if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
					if (prefs.getStorageDownloadLocation() != null){
						if (prefs.getStorageDownloadLocation().compareTo("") != 0){
							askMe = false;
							downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
						}
					}
				}
			}
		}		
			
		if (askMe){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File[] fs = getExternalFilesDirs(null);
				if (fs.length > 1){
					if (fs[1] == null){
						Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
						intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
						intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
						intent.setClass(this, FileStorageActivityLollipop.class);
						intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
						startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
					}
					else{
						Dialog downloadLocationDialog;
						String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
				        AlertDialog.Builder b=new AlertDialog.Builder(this);
	
						b.setTitle(getResources().getString(R.string.settings_storage_download_location));
						final long sizeFinal = size;
						final long[] hashesFinal = new long[hashes.length];
						for (int i=0; i< hashes.length; i++){
							hashesFinal[i] = hashes[i];
						}
						
						b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch(which){
									case 0:{
										Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
										intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
										intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, sizeFinal);
										intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
										intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashesFinal);
										startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
										break;
									}
									case 1:{
										File[] fs = getExternalFilesDirs(null);
										if (fs.length > 1){
											String path = fs[1].getAbsolutePath();
											File defaultPathF = new File(path);
											defaultPathF.mkdirs();
											Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
											downloadTo(path, null, sizeFinal, hashesFinal);
										}
										break;
									}
								}
							}
						});
						b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						});
						downloadLocationDialog = b.create();
						downloadLocationDialog.show();
					}
				}
				else{
					Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
					intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
					intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
					intent.setClass(this, FileStorageActivityLollipop.class);
					intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
					startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
				}
			}
			else{
				Intent intent = new Intent(Mode.PICK_FOLDER.getAction());
				intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
				intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
				intent.setClass(this, FileStorageActivityLollipop.class);
				intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
				startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
			}
		}
		else{
			downloadTo(downloadLocationDefaultPath, null, size, hashes);
		}
	}
	
	@Override
	public void onSaveInstanceState (Bundle savedInstanceState){
		super.onSaveInstanceState(savedInstanceState);

		savedInstanceState.putInt("adapterType", adapterType);
		if ((adapterType == Constants.OFFLINE_ADAPTER) || (adapterType == Constants.ZIP_ADAPTER)){
			
		}
		else{
			savedInstanceState.putBoolean("aBshown", adapterMega.isaBshown());
			savedInstanceState.putBoolean("overflowVisible", adapterMega.isMenuVisible());
		}
	}
	
	@Override
	public void onRestoreInstanceState (Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		
		adapterType = savedInstanceState.getInt("adapterType");
		
		if ((adapterType == Constants.OFFLINE_ADAPTER) || (adapterType == Constants.ZIP_ADAPTER)){
			
		}
		else{
			aBshown = savedInstanceState.getBoolean("aBshown");
			adapterMega.setaBshown(aBshown);
			overflowVisible = savedInstanceState.getBoolean("overflowVisible");
			adapterMega.setMenuVisible(overflowVisible);
		}
		
		if (!aBshown){
			TranslateAnimation animBottom = new TranslateAnimation(0, 0, 0, Util.px2dp(48, outMetrics));
			animBottom.setDuration(0);
			animBottom.setFillAfter( true );
			bottomLayout.setAnimation(animBottom);
			
			TranslateAnimation animTop = new TranslateAnimation(0, 0, 0, Util.px2dp(-48, outMetrics));
			animTop.setDuration(0);
			animTop.setFillAfter( true );
			topLayout.setAnimation(animTop);
		}
		
		if(overflowMenuList != null)
		{
			if (overflowVisible){
				overflowMenuList.setVisibility(View.VISIBLE);
			}
			else{
				overflowMenuList.setVisibility(View.GONE);
			}
		}
	}

	public void showRenameDialog(){

		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);
//	    layout.setLayoutParams(params);

		final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
//		input.setId(EDIT_TEXT_ID);
		input.setSingleLine();
		input.setText(node.getName());

		input.setImeOptions(EditorInfo.IME_ACTION_DONE);

		input.setImeActionLabel(getString(R.string.context_rename),
				EditorInfo.IME_ACTION_DONE);
		
		input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(final View v, boolean hasFocus) {
				if (hasFocus) {
					if (node.isFolder()){
						input.setSelection(0, input.getText().length());
					}
					else{
						String [] s = node.getName().split("\\.");
						if (s != null){
							int numParts = s.length;
							int lastSelectedPos = 0;
							if (numParts == 1){
								input.setSelection(0, input.getText().length());
							}
							else if (numParts > 1){
								for (int i=0; i<(numParts-1);i++){
									lastSelectedPos += s[i].length(); 
									lastSelectedPos++;
								}
								lastSelectedPos--; //The last point should not be selected)
								input.setSelection(0, lastSelectedPos);
							}
						}
						showKeyboardDelayed(v);
					}
				}
			}
		});

		layout.addView(input, params);

		LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		params1.setMargins(Util.scaleWidthPx(20, outMetrics), 0, Util.scaleWidthPx(17, outMetrics), 0);

		final RelativeLayout error_layout = new RelativeLayout(FullScreenImageViewerLollipop.this);
		layout.addView(error_layout, params1);

		final ImageView error_icon = new ImageView(FullScreenImageViewerLollipop.this);
		error_icon.setImageDrawable(FullScreenImageViewerLollipop.this.getResources().getDrawable(R.drawable.ic_input_warning));
		error_layout.addView(error_icon);
		RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();


		params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		error_icon.setLayoutParams(params_icon);

		error_icon.setColorFilter(ContextCompat.getColor(FullScreenImageViewerLollipop.this, R.color.login_warning));

		final TextView textError = new TextView(FullScreenImageViewerLollipop.this);
		error_layout.addView(textError);
		RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
		params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
		params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params_text_error.setMargins(Util.scaleWidthPx(3, outMetrics), 0,0,0);
		textError.setLayoutParams(params_text_error);

		textError.setTextColor(ContextCompat.getColor(FullScreenImageViewerLollipop.this, R.color.login_warning));

		error_layout.setVisibility(View.GONE);

		input.getBackground().mutate().clearColorFilter();
		input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
		input.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

			}

			@Override
			public void afterTextChanged(Editable editable) {
				if(error_layout.getVisibility() == View.VISIBLE){
					error_layout.setVisibility(View.GONE);
					input.getBackground().mutate().clearColorFilter();
					input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
				}
			}
		});

		input.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
										  KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					String value = v.getText().toString().trim();
					if (value.length() == 0) {
						input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
						textError.setText(getString(R.string.invalid_string));
						error_layout.setVisibility(View.VISIBLE);
						input.requestFocus();
						return true;
					}
					rename(value);
					renameDialog.dismiss();
					return true;
				}
				return false;
			}
		});

		AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, getString(R.string.context_rename) + " "	+ new String(node.getName()), null, layout);
		builder.setPositiveButton(getString(R.string.context_rename),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() == 0) {
							return;
						}
						rename(value);
					}
				});
		builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				input.getBackground().clearColorFilter();
			}
		});
		renameDialog = builder.create();
		renameDialog.show();
		renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				String value = input.getText().toString().trim();
				if (value.length() == 0) {
					input.getBackground().mutate().setColorFilter(getResources().getColor(R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
					textError.setText(getString(R.string.invalid_string));
					error_layout.setVisibility(View.VISIBLE);
					input.requestFocus();
				}
				else{
					rename(value);
					renameDialog.dismiss();
				}
			}
		});
	}
	
	/*
	 * Display keyboard
	 */
	private void showKeyboardDelayed(final View view) {
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
			}
		}, 50);
	}
	
	private void rename(String newName){
		if (newName.equals(node.getName())) {
			return;
		}
		
		if(!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if (isFinishing()){
			return;
		}
		
		ProgressDialog temp = null;
		try{
			temp = new ProgressDialog(this);
			temp.setMessage(getString(R.string.context_renaming));
			temp.show();
		}
		catch(Exception e){
			return;
		}
		statusDialog = temp;
		
		log("renaming " + node.getName() + " to " + newName);
		
		megaApi.renameNode(node, newName, this);
	}
	
	public void showMove(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("MOVE_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
	}
	
	public void showCopy(){
		
		ArrayList<Long> handleList = new ArrayList<Long>();
		handleList.add(node.getHandle());
		
		Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
		intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
		long[] longArray = new long[handleList.size()];
		for (int i=0; i<handleList.size(); i++){
			longArray[i] = handleList.get(i);
		}
		intent.putExtra("COPY_FROM", longArray);
		startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
	}
	
	public void moveToTrash(){
		log("moveToTrash");
		
		final long handle = node.getHandle();
		moveToRubbish = false;
		if (!Util.isOnline(this)){
			Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
			return;
		}
		
		if(isFinishing()){
			return;	
		}
		
		final MegaNode rubbishNode = megaApi.getRubbishNode();
		
		MegaNode parent = megaApi.getNodeByHandle(handle);
		while (megaApi.getParentNode(parent) != null){
			parent = megaApi.getParentNode(parent);
		}
		
		if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
			moveToRubbish = true;			
		}
		else{
			moveToRubbish = false;
		}
		
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        switch (which){
		        case DialogInterface.BUTTON_POSITIVE:
		        	//TODO remove the outgoing shares
		    		//Check if the node is not yet in the rubbish bin (if so, remove it)			
		    		
		    		if (moveToRubbish){
		    			megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_move_to_trash));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		    		else{
		    			megaApi.remove(megaApi.getNodeByHandle(handle), fullScreenImageViewer);
		    			ProgressDialog temp = null;
		    			try{
		    				temp = new ProgressDialog(fullScreenImageViewer);
		    				temp.setMessage(getString(R.string.context_delete_from_mega));
		    				temp.show();
		    			}
		    			catch(Exception e){
		    				return;
		    			}
		    			statusDialog = temp;
		    		}
		        	
		            break;

		        case DialogInterface.BUTTON_NEGATIVE:
		            //No button clicked
		            break;
		        }
		    }
		};
		
		if (moveToRubbish){
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_move_to_rubbish);
			builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
		else{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
			String message= getResources().getString(R.string.confirmation_delete_from_mega);
			builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
		    	.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
		}
	}

	public void showGetLinkActivity(long handle){
		log("showGetLinkActivity");
		Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
		linkIntent.putExtra("handle", handle);
		linkIntent.putExtra("account", accountType);
		startActivity(linkIntent);
	}

	public void setIsGetLink(boolean value){
		this.isGetLink = value;
	}

	@Override
	public void onRequestStart(MegaApiJava api, MegaRequest request) {
		log("onRequestStart: " + request.getRequestString());
	}

	@SuppressLint("NewApi")
	@Override
	public void onRequestFinish(MegaApiJava api, MegaRequest request,
			MegaError e) {
		
		node = megaApi.getNodeByHandle(request.getNodeHandle());
		
		log("onRequestFinish");
		if (request.getType() == MegaRequest.TYPE_RENAME){
			
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_renamed), Snackbar.LENGTH_LONG).show();
			}			
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_renamed), Snackbar.LENGTH_LONG).show();
			}
		}
		else if (request.getType() == MegaRequest.TYPE_MOVE){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (moveToRubbish){
				if (e.getErrorCode() == MegaError.API_OK){
					if(positionToRemove!=-1){
						log("Position to remove: "+positionToRemove);
						log("Position in: "+positionG);
						imageHandles.remove(positionToRemove);
						if(imageHandles.size()==0){
							finish();
						}
						else{
							adapterMega.refreshImageHandles(imageHandles);
							viewPager.setAdapter(adapterMega);
							if(positionG>imageHandles.size()-1){
								log("Last item deleted, go to new last position");
								positionG=imageHandles.size()-1;
							}
							viewPager.setCurrentItem(positionG);
							positionToRemove=-1;
						}
					}
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				moveToRubbish = false;
				log("move to rubbish request finished");
			}
			else{
				if (e.getErrorCode() == MegaError.API_OK){
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_moved), Snackbar.LENGTH_LONG).show();
					finish();
				}
				else{
					Snackbar.make(fragmentContainer, getString(R.string.context_no_moved), Snackbar.LENGTH_LONG).show();
				}
				log("move nodes request finished");
			}
		}
		else if (request.getType() == MegaRequest.TYPE_REMOVE){
			
			
			if (e.getErrorCode() == MegaError.API_OK){
				if (statusDialog.isShowing()){
					try { 
						statusDialog.dismiss();	
					} 
					catch (Exception ex) {}
					Snackbar.make(fragmentContainer, getString(R.string.context_correctly_removed), Snackbar.LENGTH_LONG).show();
				}
				finish();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_removed), Snackbar.LENGTH_LONG).show();
			}
			log("remove request finished");
		}
		else if (request.getType() == MegaRequest.TYPE_COPY){
			try { 
				statusDialog.dismiss();	
			} 
			catch (Exception ex) {}
			
			if (e.getErrorCode() == MegaError.API_OK){
				Snackbar.make(fragmentContainer, getString(R.string.context_correctly_copied), Snackbar.LENGTH_LONG).show();
			}
			else{
				Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
			}
			log("copy nodes request finished");
		}
	}

	@Override
	public void onRequestTemporaryError(MegaApiJava api, MegaRequest request,
			MegaError e) {
		log("onRequestTemporaryError: " + request.getRequestString());	
	}
	
	public static void log(String message) {
		Util.log("FullScreenImageViewerLollipop", message);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		overflowMenuList.setVisibility(View.GONE);
		overflowVisible = false;
		adapterMega.setMenuVisible(overflowVisible);
		
		switch(position){
			case 0:{
				showRenameDialog();
				break;
			}
			case 1:{
				showMove();
				break;
			}
			case 2:{
				showCopy();
				break;
			}
			case 3:{
				positionToRemove = positionG;
				moveToTrash();
				break;
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (intent == null) {
			return;
		}
		
		if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
			log("local folder selected");
			String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
			String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
			long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
			long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
			log("URL: " + url + "___SIZE: " + size);
			
			downloadTo (parentPath, url, size, hashes);
			Snackbar.make(fragmentContainer, getString(R.string.download_began), Snackbar.LENGTH_LONG).show();
		}
		else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {
			
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
			final long toHandle = intent.getLongExtra("MOVE_TO", 0);
			final int totalMoves = moveHandles.length;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			moveToRubbish = false;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_moving));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			for(int i=0; i<moveHandles.length;i++){
				megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
			}
		}
		else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
			if(!Util.isOnline(this)){
				Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
				return;
			}
			
			final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
			final long toHandle = intent.getLongExtra("COPY_TO", 0);
			final int totalCopy = copyHandles.length;
			
			ProgressDialog temp = null;
			try{
				temp = new ProgressDialog(this);
				temp.setMessage(getString(R.string.context_copying));
				temp.show();
			}
			catch(Exception e){
				return;
			}
			statusDialog = temp;
			
			MegaNode parent = megaApi.getNodeByHandle(toHandle);
			for(int i=0; i<copyHandles.length;i++){
				MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
				if (cN != null){
					log("cN != null, i = " + i + " of " + copyHandles.length);
					megaApi.copyNode(cN, parent, this);
				}
				else{
					log("cN == null, i = " + i + " of " + copyHandles.length);
					try {
						statusDialog.dismiss();
						Snackbar.make(fragmentContainer, getString(R.string.context_no_copied), Snackbar.LENGTH_LONG).show();
					}
					catch (Exception ex) {}
				}
			}
		}
	}
	
	/*
	 * Get list of all child files
	 */
	private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {
		
		if (megaApi.getRootNode() == null)
			return;
		
		folder.mkdir();
		ArrayList<MegaNode> nodeList = megaApi.getChildren(parent, orderGetChildren);
		for(int i=0; i<nodeList.size(); i++){
			MegaNode document = nodeList.get(i);
			if (document.getType() == MegaNode.TYPE_FOLDER) {
				File subfolder = new File(folder, new String(document.getName()));
				getDlList(dlFiles, document, subfolder);
			} 
			else {
				dlFiles.put(document, folder.getAbsolutePath());
			}
		}
	}

	@Override
	public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
		// TODO Auto-generated method stub
		
	}
	
	public void downloadTo(String parentPath, String url, long size, long [] hashes){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
			if (!hasStoragePermission) {
				ActivityCompat.requestPermissions(this,
		                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
						Constants.REQUEST_WRITE_STORAGE);
			}
		}
		
		double availableFreeSpace = Double.MAX_VALUE;
		try{
			StatFs stat = new StatFs(parentPath);
			availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
		}
		catch(Exception ex){}
		
		
		if (hashes == null){
			if(url != null) {
				if(availableFreeSpace < size) {
					Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
					return;
				}
				
				Intent service = new Intent(this, DownloadService.class);
				service.putExtra(DownloadService.EXTRA_URL, url);
				service.putExtra(DownloadService.EXTRA_SIZE, size);
				service.putExtra(DownloadService.EXTRA_PATH, parentPath);
				service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
				startService(service);
			}
		}
		else{
			if(hashes.length == 1){
				MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
				if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
					log("ISFILE");
					String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
					if(localPath != null){	
						try { 
							Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName())); 
						}
						catch(Exception e) {}

						try {

							Intent viewIntent = new Intent(Intent.ACTION_VIEW);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
								viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							} else {
								viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
							}
							viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
							if (MegaApiUtils.isIntentAvailable(this, viewIntent))
								startActivity(viewIntent);
							else {
								Intent intentShare = new Intent(Intent.ACTION_SEND);
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
									intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
								} else {
									intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
								}
								intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
								if (MegaApiUtils.isIntentAvailable(this, intentShare))
									startActivity(intentShare);
								String message = getString(R.string.general_already_downloaded) + ": " + localPath;
								Snackbar.make(fragmentContainer, message, Snackbar.LENGTH_LONG).show();
							}
						}
						catch (Exception e){
							String message = getString(R.string.general_already_downloaded) + ": " + localPath;
							Snackbar.make(fragmentContainer, message, Snackbar.LENGTH_LONG).show();
						}
						return;
					}
				}
			}
			
			for (long hash : hashes) {
				MegaNode node = megaApi.getNodeByHandle(hash);
				if(node != null){
					Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
					if (node.getType() == MegaNode.TYPE_FOLDER) {
						getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
					} else {
						dlFiles.put(node, parentPath);
					}
					
					for (MegaNode document : dlFiles.keySet()) {
						
						String path = dlFiles.get(document);
						
						if(availableFreeSpace < document.getSize()){
							Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
							continue;
						}
						
						Intent service = new Intent(this, DownloadService.class);
						service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
						service.putExtra(DownloadService.EXTRA_URL, url);
						service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
						service.putExtra(DownloadService.EXTRA_PATH, path);
						service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
						startService(service);
					}
				}
				else if(url != null) {
					if(availableFreeSpace < size) {
						Snackbar.make(fragmentContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
						continue;
					}
					
					Intent service = new Intent(this, DownloadService.class);
					service.putExtra(DownloadService.EXTRA_HASH, hash);
					service.putExtra(DownloadService.EXTRA_URL, url);
					service.putExtra(DownloadService.EXTRA_SIZE, size);
					service.putExtra(DownloadService.EXTRA_PATH, parentPath);
					service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
					startService(service);
				}
				else {
					log("node not found");
				}
			}
		}
	}

	public void showSnackbar(String s){
		log("showSnackbar");
		Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
		TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
		snackbarTextView.setMaxLines(5);
		snackbar.show();
	}
	
	@Override
	public void onBackPressed() {
		((MegaApplication) getApplication()).sendSignalPresenceActivity();

		if (overflowMenuList != null){
			if (overflowMenuList.getVisibility() == View.VISIBLE){
				overflowMenuList.setVisibility(View.GONE);
				return;
			}
		}
		super.onBackPressed();
	}
}
