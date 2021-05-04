# IBM Cloud App Configuration Java server SDK

IBM Cloud App Configuration SDK is used to perform feature flag and property evaluation based on the configuration on IBM Cloud App Configuration service.

## Table of Contents

  - [Overview](#overview)
  - [Installation](#Installation)
  - [Import the SDK](#import-the-sdk)
  - [Initialize SDK](#initialize-sdk)
  - [License](#license)

## Overview

IBM Cloud App Configuration is a centralized feature management and configuration service on [IBM Cloud](https://www.cloud.ibm.com) for use with web and mobile applications, microservices, and distributed environments.

Instrument your applications with App Configuration Java SDK, and use the App Configuration dashboard, CLI or API to define feature flags or properties, organized into collections and targeted to segments. Toggle feature flag states in the cloud to activate or deactivate features in your application or environment, when required. You can also manage the properties for distributed applications centrally.


## Installation

### maven 

```xml
<dependency>
    <groupId>com.ibm.cloud</groupId>
    <artifactId>appconfiguration-java-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle

```sh
implementation group: 'com.ibm.cloud', name: 'appconfiguration-java-sdk', version: '2.0.0'
```

## Import the SDK

```java
import com.ibm.cloud.appconfiguration.sdk.AppConfiguration
```

## Initialize SDK

```java
AppConfiguration appConfiguration = AppConfiguration.getInstance();

String guid =  "guid"
String apikey = "apikey";

appConfiguration.init(AppConfiguration.REGION_US_SOUTH, guid, apikey);

String collectionId = "collectionId";
String environmentId = "environmentId";

appConfiguration.setContext(collectionId, environmentId)
```

- region : Region name where the service instance is created. Use
    - `AppConfiguration.REGION_US_SOUTH` for Dallas
    - `AppConfiguration.REGION_EU_GB` for London
    - `AppConfiguration.REGION_AU_SYD` for Sydney
- guid : GUID of the App Configuration service. Get it from the service instance credentials section of the dashboard
- apikey : ApiKey of the App Configuration service. Get it from the service instance credentials section of the dashboard
- collectionId : Id of the collection created in App Configuration service instance under the **Collections** section.
- environmentId : Id of the environment created in App Configuration service instance under the **Environments** section.

> Here, by default live update from the server is enabled. To turn off this mode see the [below section](#work-offline-with-local-configuration-file)

### Work offline with local configuration file
You can also work offline with local configuration file and perform feature and property related operations.

```java

String configurationFile = "custom/userJson.json";
Boolean liveConfigUpdateEnabled = true;

appConfiguration.setContext(collectionId, environmentId, configurationFile, liveConfigUpdateEnabled);

```
- configurationFile : Path to the JSON file which contains configuration details.
- liveConfigUpdateEnabled : Set this value to false if the new configuration values shouldn't be fetched from the server. Make sure to provide a proper JSON file in the configurationFile path. By default, this value is enabled.

## Get single feature

```java
Feature feature = appConfiguration.getFeature("feature_id");

if (feature) {
    System.out.println("Feature Name : " + feature.getFeatureName());
    System.out.println("Feature Id : " + feature.getFeatureId());
    System.out.println("Feature Type : " + feature.getFeatureDataType());
    System.out.println("Feature is enabled : " + feature.isEnabled());
}
```

## Get all features 

```java
HashMap<String, Feature> features = appConfiguration.getFeatures();
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

## Get single property

```java
Property property = appConfiguration.getProperty("property_id");

if (property) {
    System.out.println("Property Name : " + property.getPropertyName());
    System.out.println("Property Id : " + property.getPropertyId());
    System.out.println("Property Type : " + property.getPropertyDataType());
}
```

## Get all properties 

```java
HashMap<String, Property> property = appConfiguration.getProperties();
```

## Evaluate a property 

You can use the property.getCurrentValue(identityId, identityAttributes) method to evaluate the value of the property. 

You should pass an unique identityId as the parameter to perform the property evaluation. If the property is configured with segments in the App Configuration service, you can set the attributes values as a JSONObject.

```java

JSONObject identityAttributes = new JSONObject();
identityAttributes.put("city", "Bangalore");
identityAttributes.put("country", "India");

String value = (String) property.getCurrentValue("identityId", identityAttributes);
```

## Set listener for feature or property data changes

To listen to the data changes add the following code in your application.

```java
appConfiguration.registerConfigurationUpdateListener(new ConfigurationUpdateListener() {
    @Override
    public void onConfigurationUpdate() {
       System.out.println("Got feature/property now");
    }
});
```

## Fetch latest data 

```java
appConfiguration.fetchConfigurations()
```

## Enable debugger (Optional)

```py
appConfiguration.enableDebug(True)
```

## License

This project is released under the Apache 2.0 license. The license's full text can be found in [LICENSE](https://github.com/IBM/appconfiguration-sdk-java/blob/master/LICENSE)
