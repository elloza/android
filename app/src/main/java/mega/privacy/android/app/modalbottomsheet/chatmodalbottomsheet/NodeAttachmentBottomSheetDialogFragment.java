package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class NodeAttachmentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;
    MegaNodeList nodeList;
    AndroidMegaChatMessage message = null;
    long chatId;
    long messageId;
    long handle=-1;
    ChatController chatC;

    private BottomSheetBehavior mBehavior;

    LinearLayout mainLinearLayout;
    CoordinatorLayout coordinatorLayout;

    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    RelativeLayout nodeIconLayout;
    ImageView nodeIcon;
    LinearLayout optionView;
    TextView optionViewText;
    LinearLayout optionDownload;
    LinearLayout optionImport;
    LinearLayout optionSaveOffline;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            log("Handle of the chat: "+chatId);
            messageId = savedInstanceState.getLong("messageId", -1);
            log("Handle of the message: "+messageId);
            handle = savedInstanceState.getLong("handle", -1);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if(messageMega!=null){
                message = new AndroidMegaChatMessage(messageMega);
            }
        }
        else{
            log("Bundle NULL");

            if(context instanceof ChatActivityLollipop){
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
            }
            else{
                chatId = ((NodeAttachmentActivityLollipop) context).chatId;
                messageId = ((NodeAttachmentActivityLollipop) context).messageId;
                handle = ((NodeAttachmentActivityLollipop) context).selectedNode.getHandle();
            }

            log("Id Chat and Message id: "+chatId+ "___"+messageId);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);
            if(messageMega!=null){
                message = new AndroidMegaChatMessage(messageMega);
            }
        }

        chatC = new ChatController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        log("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_attachment_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.node_attachment_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.node_attachment_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.node_attachment_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.node_attachment_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.node_attachment_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.node_attachment_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionView = (LinearLayout) contentView.findViewById(R.id.option_view_layout);
        optionViewText = (TextView) contentView.findViewById(R.id.option_view_text);
        optionSaveOffline = (LinearLayout) contentView.findViewById(R.id.option_save_offline_layout);
        optionImport = (LinearLayout) contentView.findViewById(R.id.option_import_layout);

        optionDownload.setOnClickListener(this);
        optionView.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionImport.setOnClickListener(this);

        nodeIconLayout.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            log("onCreate: Landscape configuration");
            nodeName.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
        }
        else{
            nodeName.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
        }

        if (message != null) {
            nodeList = message.getMessage().getMegaNodeList();

            if(nodeList==null){
                log("Error, nodeList is NULL");
                return;
            }

            if(handle==-1){
                node = nodeList.get(0);
            }
            else{
                node = getNodeByHandle(handle);
            }

            if(node!=null) {
                log("node is NOT null");

                if(handle==-1){
                    log("Panel shown from ChatActivity");
                    if(nodeList.size()==1){
                        log("one file included");
                        if (Util.isOnline(context)) {

                            if (node.hasThumbnail()) {
                                log("Node has thumbnail");
                                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                params1.setMargins(20, 0, 12, 0);
                                nodeThumb.setLayoutParams(params1);

                                thumb = ThumbnailUtils.getThumbnailFromCache(node);
                                if (thumb != null) {
                                    nodeThumb.setImageBitmap(thumb);
                                } else {
                                    thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                                    if (thumb != null) {
                                        nodeThumb.setImageBitmap(thumb);
                                    } else {
                                        nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                    }
                                }
                            }
                            else {
                                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                            }
                        }

                        nodeName.setText(node.getName());

                        long nodeSize = node.getSize();
                        nodeInfo.setText(Util.getSizeString(nodeSize));

                        optionView.setVisibility(View.GONE);
                    }
                    else{
                        log("Several nodes in the message");
                        optionView.setVisibility(View.VISIBLE);

                        long totalSize = 0;
                        int count = 0;
                        for(int i=0; i<nodeList.size(); i++){
                            MegaNode temp = nodeList.get(i);
                            if(!(megaChatApi.isRevoked(chatId, temp.getHandle()))){
                                count++;
                                log("Node Name: "+temp.getName());
                                totalSize = totalSize + temp.getSize();
                            }
                        }
                        nodeInfo.setText(Util.getSizeString(totalSize));
                        MegaNode node = nodeList.get(0);
                        nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        if(count==1){
                            nodeName.setText(node.getName());
                        }
                        else{
                            nodeName.setText(context.getResources().getQuantityString(R.plurals.new_general_num_files, count, count));
                        }

                        if(nodeList.size()==count){
                            optionViewText.setText(getString(R.string.general_view));
                        }
                        else{
                            optionViewText.setText(getString(R.string.general_view_with_revoke, nodeList.size()-count));
                        }
                    }
                }
                else{
                    log("Panel shown from NodeAttachmenntActivity - always one file selected");
                    if (Util.isOnline(context)) {

                        if (node.hasThumbnail()) {
                            log("Node has thumbnail");
                            RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                            params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                            params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                            params1.setMargins(20, 0, 12, 0);
                            nodeThumb.setLayoutParams(params1);

                            thumb = ThumbnailUtils.getThumbnailFromCache(node);
                            if (thumb != null) {
                                nodeThumb.setImageBitmap(thumb);
                            } else {
                                thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                                if (thumb != null) {
                                    nodeThumb.setImageBitmap(thumb);
                                } else {
                                    nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                                }
                            }
                        }
                        else {
                            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }

                    nodeName.setText(node.getName());

                    long nodeSize = node.getSize();
                    nodeInfo.setText(Util.getSizeString(nodeSize));

                    optionView.setVisibility(View.GONE);
                }

                dialog.setContentView(contentView);

                mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
                mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            else{
                log("node is NULL");
            }
        }
    }

    public MegaNode getNodeByHandle(long handle){
        for(int i=0;i<nodeList.size();i++){
            MegaNode node = nodeList.get(i);
            if(node.getHandle()==handle){
                return node;
            }
        }
        return null;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                log("Download option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                if(context instanceof ChatActivityLollipop){
                    chatC.prepareForChatDownload(nodeList);
                }
                else if(context instanceof NodeAttachmentActivityLollipop){
                    chatC.prepareForChatDownload(node);
                }
                break;
            }
            case R.id.option_import_layout:{
                log("Import option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                if(context instanceof ChatActivityLollipop){
                    ((ChatActivityLollipop)context).importNode();
                }
                else if(context instanceof NodeAttachmentActivityLollipop){
                    ((NodeAttachmentActivityLollipop)context).importNode();
                }

                break;
            }
            case R.id.option_view_layout:{
                log("View option");
                Intent i = new Intent(context, NodeAttachmentActivityLollipop.class);
                i.putExtra("chatId", chatId);
                i.putExtra("messageId", messageId);
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_save_offline_layout:{
                log("Save for offline option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                if(context instanceof ChatActivityLollipop){

                    if(message!=null){
                        chatC.saveForOffline(message.getMessage());
                    }
                    else{
                        log("Message is NULL");
                    }
                }
                else if(context instanceof NodeAttachmentActivityLollipop){
                    chatC.saveForOffline(((NodeAttachmentActivityLollipop) context).selectedNode);
                }

                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
        super.onAttach(activity);
        this.context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putLong("chatId", chatId);
        outState.putLong("messageId", messageId);
        outState.putLong("handle", handle);
    }

    private static void log(String log) {
        Util.log("NodeAttachmentBottomSheetDialogFragment", log);
    }
}
