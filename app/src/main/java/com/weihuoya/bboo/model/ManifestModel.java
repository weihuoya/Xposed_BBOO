package com.weihuoya.bboo.model;

import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import com.weihuoya.bboo._G;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangwei on 2016/5/13.
 */
public class ManifestModel {
    public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    public class MetaDataModel {
        public String name;
        public String value;
    }

    public class ActionModel {
        public String name;
    }

    public class CategoryModel {
        public String name;
    }

    public class PathPermissionModel {
        public String path;
        public String permission;
    }

    public class IntentFilterModel {
        public int icon;
        public String label;

        public List<ActionModel> actions;
        public List<CategoryModel> categories;

        public Drawable getIcon() {
            return mPackage.getDrawable(icon);
        }
    }

    public class ActivityModel {
        public int icon;
        public String label;
        public String name;
        public boolean isReceiver;

        public List<IntentFilterModel> intentFilters;
        public List<MetaDataModel> metaDatas;
    }

    public class ServiceModel {
        public int icon;
        public String label;
        public String name;

        public List<IntentFilterModel> intentFilters;
        public List<MetaDataModel> metaDatas;
    }

    public class ProviderModel {
        public int icon;
        public String label;
        public String name;
        public String authorities;

        public List<IntentFilterModel> intentFilters;
        public List<MetaDataModel> metaDatas;
        public List<PathPermissionModel> pathPermissions;
    }

    public class ApplicationModel {
        public int icon;
        public String label;
        public String name;
        public String description;

        public List<ActivityModel> activitys;
        public List<ProviderModel> providers;
        public List<ServiceModel> services;
        public List<MetaDataModel> metaDatas;
    }

    public class UsesPermissionModel {
        public String name;
    }

    public class UsesSdkModel {
        public int minSdkVersion;
        public int targetSdkVersion;
        public int maxSdkVersion;
    }

    public String packageName;
    public List<UsesPermissionModel> usesPermissions;
    public List<IntentFilterModel> intentFilters;
    public UsesSdkModel usesSdk;
    public ApplicationModel application;

    public ManifestModel(PackageModel pkg) {
        mPackage = pkg;
    }

    public void loadManifest(XmlResourceParser parser) throws XmlPullParserException, IOException {
        int eventType;
        String tagName;
        ManifestModel manifestModel = this;

        _G.log("$$$ AppInfoLoadTask loadAppManifest");

        while ((eventType = parser.next()) != XmlPullParser.START_TAG &&
                eventType != XmlPullParser.END_DOCUMENT) {
        }

        if(eventType == XmlPullParser.START_TAG && parser.getName().equals("manifest")) {
            packageName = getAttributeString(parser, null, "package");
        }

        // Manifest
        int outerDepth = parser.getDepth();
        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT &&
                (eventType != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "application":
                    parseApplicationTag(parser, manifestModel.application);
                    break;
                case "uses-permission":
                    UsesPermissionModel usesPermissionModel = new UsesPermissionModel();
                    parseUsesPermissionTag(parser, usesPermissionModel);
                    if(manifestModel.usesPermissions == null) {
                        manifestModel.usesPermissions = new ArrayList<>();
                    }
                    manifestModel.usesPermissions.add(usesPermissionModel);
                    break;
                case "uses-sdk":
                    parseUsesSdkTag(parser, manifestModel.usesSdk);
                    break;
                /*case "overlay":
                case "key-sets":
                case "permission-group":
                case "permission":
                case "permission-tree":
                case "uses-configuration":
                case "uses-feature":
                case "feature-group":
                case "supports-screens":
                case "protected-broadcast":
                case "instrumentation":
                case "original-package":
                case "adopt-permissions":
                    skipCurrentTag(parser);
                    break;*/
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
    }

    private void parseApplicationTag(XmlPullParser parser, ApplicationModel application) throws XmlPullParserException, IOException {
        int eventType;
        String tagName;
        final int innerDepth = parser.getDepth();

        application.icon = getAttributeInteger(parser, ANDROID_NAMESPACE, "icon");
        application.label = getAttributeString(parser, ANDROID_NAMESPACE, "label");
        application.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
        application.description = getAttributeString(parser, ANDROID_NAMESPACE, "description");

        while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT
                && (eventType != XmlPullParser.END_TAG || parser.getDepth() > innerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "activity":
                case "receiver":
                    ActivityModel activityModel = new ActivityModel();
                    parseActivityTag(parser, activityModel);
                    if(application.activitys == null) {
                        application.activitys = new ArrayList<>();
                    }
                    application.activitys.add(activityModel);
                    break;
                case "service":
                    ServiceModel serviceModel = new ServiceModel();
                    parseServiceTag(parser, serviceModel);
                    if(application.services == null) {
                        application.services = new ArrayList<>();
                    }
                    application.services.add(serviceModel);
                    break;
                case "provider":
                    ProviderModel providerModel = new ProviderModel();
                    parseProviderTag(parser, providerModel);
                    if(application.providers == null) {
                        application.providers = new ArrayList<>();
                    }
                    application.providers.add(providerModel);
                    break;
                case "meta-data":
                    MetaDataModel metaDataModel = new MetaDataModel();
                    parseMetaDataTag(parser, metaDataModel);
                    if(application.metaDatas == null) {
                        application.metaDatas = new ArrayList<>();
                    }
                    application.metaDatas.add(metaDataModel);
                    break;
                /*case "activity-alias":
                case "library":
                case "uses-library":
                case "uses-package":
                    skipCurrentTag(parser);
                    break;*/
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
    }

    private void parseUsesPermissionTag(XmlPullParser parser, UsesPermissionModel model) throws XmlPullParserException, IOException {
        model.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
        skipCurrentTag(parser);
    }

    private void parseUsesSdkTag(XmlPullParser parser, UsesSdkModel usesSdk) throws XmlPullParserException, IOException {
        usesSdk.minSdkVersion = getAttributeInteger(parser, ANDROID_NAMESPACE, "minSdkVersion");
        usesSdk.targetSdkVersion = getAttributeInteger(parser, ANDROID_NAMESPACE, "targetSdkVersion");
        usesSdk.maxSdkVersion = getAttributeInteger(parser, ANDROID_NAMESPACE, "maxSdkVersion");
        skipCurrentTag(parser);
    }

    private void parseActivityTag(XmlPullParser parser, ActivityModel activityModel) throws XmlPullParserException, IOException {
        String tagName = parser.getName();

        if(tagName.equals("receiver")) {
            activityModel.isReceiver = true;
        }

        activityModel.icon = getAttributeInteger(parser, ANDROID_NAMESPACE, "icon");
        activityModel.label = getAttributeString(parser, ANDROID_NAMESPACE, "label");
        activityModel.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");

        int eventType;
        int outerDepth = parser.getDepth();
        while ((eventType=parser.next()) != XmlPullParser.END_DOCUMENT &&
                (eventType != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "intent-filter":
                    IntentFilterModel intentFilterModel = new IntentFilterModel();
                    parseIntentFilterTag(parser, intentFilterModel);
                    if(activityModel.intentFilters == null) {
                        activityModel.intentFilters = new ArrayList<>();
                    }
                    activityModel.intentFilters.add(intentFilterModel);
                    break;
                case "meta-data":
                    MetaDataModel metaDataModel = new MetaDataModel();
                    parseMetaDataTag(parser, metaDataModel);
                    if(activityModel.metaDatas == null) {
                        activityModel.metaDatas = new ArrayList<>();
                    }
                    activityModel.metaDatas.add(metaDataModel);
                    break;
                /*case "preferred":
                    skipCurrentTag(parser);
                    break;*/
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
    }

    private void parseServiceTag(XmlPullParser parser, ServiceModel serviceModel) throws XmlPullParserException, IOException {
        int eventType;
        String tagName;
        int outerDepth = parser.getDepth();

        serviceModel.icon = getAttributeInteger(parser, ANDROID_NAMESPACE, "icon");
        serviceModel.label = getAttributeString(parser, ANDROID_NAMESPACE, "label");
        serviceModel.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");

        while ((eventType=parser.next()) != XmlPullParser.END_DOCUMENT &&
                (eventType != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "intent-filter":
                    IntentFilterModel intentFilterModel = new IntentFilterModel();
                    parseIntentFilterTag(parser, intentFilterModel);
                    if(serviceModel.intentFilters == null) {
                        serviceModel.intentFilters = new ArrayList<>();
                    }
                    serviceModel.intentFilters.add(intentFilterModel);
                    break;
                case "meta-data":
                    MetaDataModel metaDataModel = new MetaDataModel();
                    parseMetaDataTag(parser, metaDataModel);
                    if(serviceModel.metaDatas == null) {
                        serviceModel.metaDatas = new ArrayList<>();
                    }
                    serviceModel.metaDatas.add(metaDataModel);
                    break;
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
    }

    private void parseProviderTag(XmlPullParser parser, ProviderModel providerModel) throws XmlPullParserException, IOException {
        int eventType;
        String tagName;
        int outerDepth = parser.getDepth();

        providerModel.icon = getAttributeInteger(parser, ANDROID_NAMESPACE, "icon");
        providerModel.label = getAttributeString(parser, ANDROID_NAMESPACE, "label");
        providerModel.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
        providerModel.authorities = getAttributeString(parser, ANDROID_NAMESPACE, "authorities");

        while ((eventType=parser.next()) != XmlPullParser.END_DOCUMENT &&
                (eventType != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "intent-filter":
                    IntentFilterModel intentFilterModel = new IntentFilterModel();
                    parseIntentFilterTag(parser, intentFilterModel);
                    if(providerModel.intentFilters == null) {
                        providerModel.intentFilters = new ArrayList<>();
                    }
                    providerModel.intentFilters.add(intentFilterModel);
                    break;
                case "meta-data":
                    MetaDataModel metaDataModel = new MetaDataModel();
                    parseMetaDataTag(parser, metaDataModel);
                    if(providerModel.metaDatas == null) {
                        providerModel.metaDatas = new ArrayList<>();
                    }
                    providerModel.metaDatas.add(metaDataModel);
                    break;
                case "path-permission":
                    PathPermissionModel pathPermissionModel = new PathPermissionModel();
                    pathPermissionModel.path = getAttributeString(parser, ANDROID_NAMESPACE, "path");
                    pathPermissionModel.permission = getAttributeString(parser, ANDROID_NAMESPACE, "permission");
                    if(providerModel.pathPermissions == null) {
                        providerModel.pathPermissions = new ArrayList<>();
                    }
                    providerModel.pathPermissions.add(pathPermissionModel);
                    break;
                /*case "grant-uri-permission":
                    skipCurrentTag(parser);
                    break;*/
                default:
                    skipCurrentTag(parser);
                    break;
            }
        }
    }

    private void parseIntentFilterTag(XmlPullParser parser, IntentFilterModel model) throws XmlPullParserException, IOException {
        int eventType;
        String tagName;
        int outerDepth = parser.getDepth();

        model.icon = getAttributeInteger(parser, ANDROID_NAMESPACE, "icon");
        model.label = getAttributeString(parser, ANDROID_NAMESPACE, "label");

        while ((eventType=parser.next()) != XmlPullParser.END_DOCUMENT &&
                (eventType != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (eventType == XmlPullParser.END_TAG || eventType == XmlPullParser.TEXT) {
                continue;
            }

            tagName = parser.getName();
            switch (tagName) {
                case "action":
                    ActionModel actionModel = new ActionModel();
                    actionModel.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
                    if(model.actions == null) {
                        model.actions = new ArrayList<>();
                    }
                    model.actions.add(actionModel);
                    break;
                case "category":
                    CategoryModel categoryModel = new CategoryModel();
                    categoryModel.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
                    if(model.categories == null) {
                        model.categories = new ArrayList<>();
                    }
                    model.categories.add(categoryModel);
                    break;
                case "data":
                    skipCurrentTag(parser);
                    break;
            }
        }

        if(intentFilters == null) {
            intentFilters = new ArrayList<>();
        }
        intentFilters.add(model);
    }

    private void parseMetaDataTag(XmlPullParser parser, MetaDataModel model) throws XmlPullParserException, IOException {
        model.name = getAttributeString(parser, ANDROID_NAMESPACE, "name");
        model.value = getAttributeString(parser, ANDROID_NAMESPACE, "value");
        skipCurrentTag(parser);
    }

    private void skipCurrentTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        int type;
        int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT &&
                (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
        }
    }

    private int getAttributeInteger(XmlPullParser parser, String ns, String name) {
        String text = parser.getAttributeValue(ns, name);

        if(text != null) {
            if(text.startsWith("@")) {
                text = text.substring(1);
            }
            return Integer.parseInt(text);
        } else {
            return -1;
        }
    }

    private String getAttributeString(XmlPullParser parser, String ns, String name) {
        String text = parser.getAttributeValue(ns, name);

        if(text != null) {
            if(text.startsWith("@")) {
                int resId = Integer.parseInt(text.substring(1));
                text = mPackage.getString(resId);
            } else if(name.equals("name") && text.startsWith(".") && packageName != null) {
                text = packageName + text;
            }
        }

        return text;
    }

    private PackageModel mPackage;
}
