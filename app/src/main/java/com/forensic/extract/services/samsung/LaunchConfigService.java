package com.forensic.extract.services.samsung;

import android.accessibilityservice.AccessibilityService;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.forensic.extract.services.AccessibleService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LaunchConfigService extends AccessibilityService {
    private boolean finish = false;
    private PageStep step;
    private static final String TAG = "AccService";

    public void onServiceConnected() {

    }

    private void traverseNodeInfo(AccessibilityNodeInfo source, int depth) {
        if(source == null)
            return;

        CharSequence description = source.getContentDescription();
        if(description != null)
            Log.d(TAG, "Node: " + source.getClassName() + ", Description: " + description.toString());

        // 遍历子节点
        for (int i = 0; i < source.getChildCount(); i++) {
            traverseNodeInfo(source.getChild(i), depth + 1);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();

        traverseNodeInfo(source,0);

        if(!this.finish && source != null) {
            int eventType = event.getEventType();
            if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                String name = source.getViewIdResourceName();
                if(name != null)
                    Log.d(TAG, "Node: " + source.getClassName() + ", View Id: " + source.getViewIdResourceName());

                List<AccessibilityNodeInfo> infos;
                CharSequence text;

                PageStep pageStep = this.step;
                if(pageStep == null) {
                    List<AccessibilityNodeInfo> titleInfo = source.findAccessibilityNodeInfosByViewId("com.sec.android.easyMover:id/text_welcome_title");
                    if (titleInfo == null || titleInfo.size() <= 0) {
                        titleInfo = source.findAccessibilityNodeInfosByViewId("com.sec.android.easyMover:id/text_title");
                    }
                    if(titleInfo != null && titleInfo.size() > 0) {
                        this.step = PageStep.ONE;
                        source.getChild(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        source.findAccessibilityNodeInfosByViewId("com.sec.android.easyMover:id/layout_check_agreement_1").get(0).performAction(16);
                        source.findAccessibilityNodeInfosByViewId("com.sec.android.easyMover:id/layout_check_agreement_2").get(0).performAction(16);
                        source.getChild(2).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }else if(pageStep == PageStep.ONE) {
                    iterateNodesAndHandle(source);
                }else if(pageStep == PageStep.TWO){
                    infos = source.findAccessibilityNodeInfosByViewId("android:id/message");
                    if(infos==null)
                        return;
                    if (infos.size() <= 0)
                        return;
                    text = infos.get(0).getText();
                    if(text==null)
                        return;
                    if(text.toString().startsWith("Current Setting [OFF]")) {
                        source.findAccessibilityNodeInfosByViewId("android:id/button1").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        this.finish=true;
                        Log.d(TAG,"Finished!!!!!!!!!!!!!!!!!");
                    }
                }
            }

        }
    }

    @Override
    public void onInterrupt() {

    }


    private void iterateNodesAndHandle(AccessibilityNodeInfo accessibilityNodeInfo) {
        if (accessibilityNodeInfo == null) {
            return;
        }

        String viewIdResourceName = accessibilityNodeInfo.getViewIdResourceName();
        if (TextUtils.isEmpty(viewIdResourceName) || !isValidResourceId(viewIdResourceName)) {
            if (isValidClassName(accessibilityNodeInfo)) {
                CharSequence text = accessibilityNodeInfo.getText();
                CharSequence description = accessibilityNodeInfo.getContentDescription();
                if (description != null) {
                    Log.d(TAG, "Description: " + description.toString());
                }
                if (isValidText(text)) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return;
                } else if (text != null && "ApkDataMove".equals(text.toString())) {
                    this.step = PageStep.TWO;
                    AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
                    if (parent != null) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    return;
                } else if (description != null) {
                    if (description.toString().equals("更多选项")) {
                        boolean performed = accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        if(performed){
                            Log.d(TAG,"performed click button");
                        }
                        return;
                    }
                }
            }
            int childCount = accessibilityNodeInfo.getChildCount();
            for (int i = 0; i < childCount; i++) {
                iterateNodesAndHandle(accessibilityNodeInfo.getChild(i));
            }
            return;
        }
        accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private boolean isValidResourceId(String viewIdResourceName) {
        Set<String> allowedResourceIds = new HashSet<>(Arrays.asList(
                "com.sec.android.easyMover:id/button_continue_single",
                "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                "com.sec.android.easyMover:id/btn_retry",
                "com.android.permissioncontroller:id/permission_allow_button"
        ));
        return allowedResourceIds.contains(viewIdResourceName);
    }

    private boolean isValidClassName(AccessibilityNodeInfo accessibilityNodeInfo) {
        String className = accessibilityNodeInfo.getClassName().toString();
        return "android.widget.Button".equals(className) || "android.widget.TextView".equals(className);
    }

    private boolean isValidText(CharSequence text) {
        Set<String> allowedTexts = new HashSet<>(Arrays.asList(
                "仅在使用该应用时允许",
                "仅在使用此应用时允许",
                "继续",
                "允许",
                "确定",
                "仅使用时允许",
                "仅本次使用时允许"
        ));
        return text != null && allowedTexts.contains(text.toString());
    }
}
