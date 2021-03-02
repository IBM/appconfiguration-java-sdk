# IBM Cloud App Configuration Java server SDK

IBM Cloud App Configuration SDK is used to perform feature evaluation based on the configuration on IBM Cloud App Configuration service.

## Table of Contents

  - [Overview](#overview)
  - [Authentication](#authentication)
  - [Import the SDK](#import-the-sdk)
  - [Initialize SDK](#initialize-sdk)
  - [License](#license)

## Overview

IBM Cloud App Configuration is a centralized feature management and configuration service on [IBM Cloud](https://www.cloud.ibm.com) for use with web and mobile applications, microservices, and distributed environments.

Instrument your applications with App Configuration Java SDK, and use the App Configuration dashboard, CLI or API to define feature flags, organized into collections and targeted to segments. Toggle feature flag states in the cloud to activate or deactivate features in your application or environment, when required.


## Installation

### maven 

```xml
<dependency>
  <groupId>com.ibm.cloud</groupId>
  <artifactId>appconfiguration-java-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Gradle

```sh
implementation group: 'com.ibm.cloud', name: 'appconfiguration-java-sdk', version: '1.0.0'
```

## Import the SDK

```java
import com.ibm.cloud.appconfiguration.sdk.AppConfiguration
```

## Authentication

In order to use an IBM App Configuration service in a Java application, you will need to authenticate. The following describes the typical path you need to take to do so.

### Step 1: Getting credentials

Credentials to use an IBM App Configuration service are obtained via IBM Cloud. You will need an active account and a service instance for the service that you wish to use prior to authenticating in your Java app.

You can access the service credentials for your instance by taking the following steps:

1. Go to the IBM Cloud [Dashboard](https://cloud.ibm.com/) page.
2. Either click an existing App Configuration service instance in your [resource list](https://cloud.ibm.com/resources) or click [**Create resource > Services > Developer Tools**](https://cloud.ibm.com/catalog?category=devops#services) and create an App Configuration service instance.
3. Click on the **Service credentials** item in the left nav bar of your App Configuration service instance.

On this page, you will see your credentials to use in the SDK to access your service instance. Get the `apikey` and `guid` from the credentials. 

### Step 2: Initialize in Code

```java
AppConfiguration appConfiguration = AppConfiguration.getInstance();

String guid =  "guid"
String apikey = "apikey";

appConfiguration.init(AppConfiguration.REGION_US_SOUTH, guid, apikey);

// Initialize feature 
String collectionId = "collectionId"; // Id of the collection created in App Configuration service instance under the **Collections** section.
appConfiguration.setCollectionId(collectionId);
```

- region : Region name where the service instance is created. Eg: `AppConfiguration.REGION_US_SOUTH` is for the Dallas(us-south) region. 
- guid : GUID of the App Configuration service. Get it from the service instance credentials section of the dashboard
- apikey : ApiKey of the App Configuration service. Get it from the service instance credentials section of the dashboard
- collectionId : Id of the collection created in App Configuration service instance under the **Collections** section.

> Here, by default live features update from the server is enabled. To turn off this mode see the [below section](#work-offline-with-local-feature-file)

## Work offline with local feature file

You can also work offline with local feature file and perform feature [operations](#get-single-feature).

After setting the collection Id, follow the below steps

```java

String featureFile = "custom/userJson.json";
Boolean liveFeatureUpdateEnabled = true;
// set the file or offline feature
appConfiguration.fetchFeaturesFromFile(featureFile, liveFeatureUpdateEnabled);
```
* featureFile : Path to the JSON file which contains feature details and segment details.
* liveFeatureUpdateEnabled : Set this value to false if the new feature values shouldn't be fetched from the server. Make sure to provide a proper JSON file in the featureFile path. By default, this value is enabled.

## Get single feature

```java
Feature feature = appConfiguration.getFeature("feature_id");
```

## Get all features 

```java
Feature feature = appConfiguration.getFeatures();
```

## Set listener for feature data changes

```java
appConfiguration.registerFeaturesUpdateListener(new FeaturesUpdateListener() {
    @Override
    public void onFeaturesUpdate() {
       System.out.println("Got feature now");
    }
});
```

## Evaluate a feature

You can use the feature.getCurrentValue(identityId, identityAttributes) method to evaluate the value of the feature flag. 

You should pass an unique identityId as the parameter to perform the feature flag evaluation. If the feature flag is configured with segments in the App Configuration service, you can set the attributes values as a JSONObject.

```java

JSONObject identityAttributes = new JSONObject();
identityAttributes.put("city", "Bangalore");
identityAttributes.put("country", "India");

String value = (String) feature.getCurrentValue("identityId", identityAttributes);
```

## Fetch latest data 

```java
appConfiguration.fetchFeatureData()
```

## Enable debugger

```py
appConfiguration.enableDebug(True)
```

## License

This project is released under the Apache 2.0 license. The license's full text can be found in [LICENSE](https://github.com/IBM/appconfiguration-sdk-java/blob/master/LICENSE)
