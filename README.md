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
    <version>0.2.2</version>
</dependency>
```

### Gradle

```sh
implementation group: 'com.ibm.cloud', name: 'appconfiguration-java-sdk', version: '0.2.2'
```

## Import the SDK

```java
import com.ibm.cloud.appconfiguration.sdk.AppConfiguration;
```

## Initialize SDK

```java
String region = "region";
String guid = "guid";
String apikey = "apikey";

String collectionId = "airlines-webapp";
String environmentId = "dev";

AppConfiguration appConfigClient = AppConfiguration.getInstance();
appConfigClient.init(region, guid, apikey);
appConfigClient.setContext(collectionId, environmentId);
```
:red_circle: **Important** :red_circle:

The **`init()`** and **`setContext()`** are the initialisation methods and should be invoked **only once** using
appConfigClient. The appConfigClient, once initialised, can be obtained across modules
using **`AppConfiguration.getInstance()`**.  [See this example below](#fetching-the-appconfigclient-across-other-modules).

- region : Region name where the service instance is created. Use
    - `AppConfiguration.REGION_US_SOUTH` for Dallas
    - `AppConfiguration.REGION_EU_GB` for London
    - `AppConfiguration.REGION_AU_SYD` for Sydney
    - `AppConfiguration.REGION_US_EAST` for Washington DC
- guid : GUID of the App Configuration service. Get it from the service instance credentials section of the dashboard
- apikey : ApiKey of the App Configuration service. Get it from the service instance credentials section of the dashboard
- collectionId : Id of the collection created in App Configuration service instance under the **Collections** section.
- environmentId : Id of the environment created in App Configuration service instance under the **Environments** section.

> Here, by default live update from the server is enabled. To turn off this mode see the [below section](#work-offline-with-local-configuration-file)

### Work offline with local configuration file
You can also work offline with local configuration file and perform feature and property related operations.

```java

String configurationFile = "saflights/flights.json";
Boolean liveConfigUpdateEnabled = false;

appConfigClient.setContext(collectionId, environmentId, configurationFile, liveConfigUpdateEnabled);

```
- configurationFile : Path to the JSON file which contains configuration details.
- liveConfigUpdateEnabled : Set this value to false if the new configuration values shouldn't be fetched from the server. Make sure to provide a proper JSON file in the configurationFile path. By default, this value is enabled.

### Permissions required by SDK
Add write permission for `non-root` users to `appconfiguration.json` file which is used as cache in AppConfiguration SDK.
## Get single feature

```java
Feature feature = appConfigClient.getFeature("online-check-in");

if (feature) {
    System.out.println("Feature Name : " + feature.getFeatureName());
    System.out.println("Feature Id : " + feature.getFeatureId());
    System.out.println("Feature Type : " + feature.getFeatureDataType());
    System.out.println("Feature is enabled : " + feature.isEnabled());
}
```

## Get all features 

```java
HashMap<String, Feature> features = appConfigClient.getFeatures();
```

## Evaluate a feature 

You can use the feature.getCurrentValue(entityId, entityAttributes) method to evaluate the value of the feature flag. 

You should pass an unique entityId as the parameter to perform the feature flag evaluation. If the feature flag is configured with segments in the App Configuration service, you can set the attributes values as a JSONObject.

```java

String entityId = "john_doe";
JSONObject entityAttributes = new JSONObject();
entityAttributes.put("city", "Bangalore");
entityAttributes.put("country", "India");

String value = (String) feature.getCurrentValue(entityId, entityAttributes);
```

## Get single property

```java
Property property = appConfigClient.getProperty("check-in-charges");

if (property) {
    System.out.println("Property Name : " + property.getPropertyName());
    System.out.println("Property Id : " + property.getPropertyId());
    System.out.println("Property Type : " + property.getPropertyDataType());
}
```

## Get all properties 

```java
HashMap<String, Property> property = appConfigClient.getProperties();
```

## Evaluate a property 

You can use the property.getCurrentValue(entityId, entityAttributes) method to evaluate the value of the property. 

You should pass an unique entityId as the parameter to perform the property evaluation. If the property is configured with segments in the App Configuration service, you can set the attributes values as a JSONObject.

```java

String entityId = "john_doe";
JSONObject entityAttributes = new JSONObject();
entityAttributes.put("city", "Bangalore");
entityAttributes.put("country", "India");

String value = (String) property.getCurrentValue(entityId, entityAttributes);
```

## Fetching the appConfigClient across other modules

Once the SDK is initialized, the appConfigClient can be obtained across other modules as shown below:

```java
// **other modules**

import com.ibm.cloud.appconfiguration.sdk.AppConfiguration;
AppConfiguration appConfigClient = AppConfiguration.getInstance();

Feature feature = appConfigClient.getFeature("string-feature");
boolean enabled = feature.isEnabled();
String featureValue = (String) feature.getCurrentValue(entityId, entityAttributes);
```

## Supported Data types

App Configuration service allows to configure the feature flag and properties in the following data types : Boolean,
Numeric, String. The String data type can be of the format of a TEXT string , JSON or YAML. The SDK processes each
format accordingly as shown in the below table.
<details><summary>View Table</summary>

| **Feature or Property value**                                                                                      | **DataType** | **DataFormat** | **Type of data returned <br> by `GetCurrentValue()`** | **Example output**                                                   |
| ------------------------------------------------------------------------------------------------------------------ | ------------ | -------------- | ----------------------------------------------------- | -------------------------------------------------------------------- |
| `true`                                                                                                             | BOOLEAN      | not applicable | `java.lang.Boolean`                                                | `true`                                                               |
| `25`                                                                                                               | NUMERIC      | not applicable | `java.lang.Integer`                                             | `25`                                                                 |
| "a string text"                                                                                                    | STRING       | TEXT           | `java.lang.String`                                              | `a string text`                                                      |
| <pre>{<br>  "firefox": {<br>    "name": "Firefox",<br>    "pref_url": "about:config"<br>  }<br>}</pre> | STRING       | JSON           | `org.json.JSONObject`                              | `{"firefox": {"name": "Firefox", "pref_url": "about:config"}}` |
| <pre>men:<br>  - John Smith<br>  - Bill Jones<br>women:<br>  - Mary Smith<br>  - Susan Williams</pre>  | STRING       | YAML           | `java.lang.String`                              | `"men:\n - John Smith\n - Bill Jones\nwomen:\n - Mary Smith\n - Susan Williams"` |

</details>

<details><summary>Feature flag</summary>

```java
Feature feature = appConfigClient.getFeature("json-feature");
if (feature != null) {
    feature.getFeatureDataType();       // STRING
    feature.getFeatureDataFormat();     // JSON
    feature.getCurrentValue(entityId, entityAttributes); // JSONObject or JSONArray is returned
}

// Example Below
// input json :- [{"role": "developer", "description": "do coding"},{"role": "tester", "description": "do testing"}]
// expected output :- "do coding"

JSONArray tar_val = (JSONArray) feature.get_current_value(entityId, entityAttributes);
String expected_output = (String) ((JSONObject) tar_val.get(0)).get('description');

// input json :- {"role": "tester", "description": "do testing"}
// expected output :- "tester"

JSONObject tar_val = (JSONObject) feature.get_current_value(entityId, entityAttributes);
String expected_output = (String) tar_val.get('role');

Feature feature = appConfigClient.getFeature("yaml-feature");
if (feature != null) {
    feature.getFeatureDataType();       // STRING
    feature.getFeatureDataFormat();     // YAML
    feature.getCurrentValue(entityId, entityAttributes); // Yaml String is returned
}
```

</details>
<details><summary>Property</summary>

```java
Property property = appConfigClient.getProperty("json-property");
if (property != null) {
    property.getPropertyDataType()     // STRING
    property.getPropertyDataFormat()   // JSON
    property.getCurrentValue(entityId, entityAttributes) // JSONObject or JSONArray is returned
    
}

// Example Below
// input json :- [{"role": "developer", "description": "do coding"},{"role": "tester", "description": "do testing"}]
// expected output :- "do coding"

JSONArray tar_val = (JSONArray) property.get_current_value(entityId, entityAttributes);
String expected_output = (String) ((JSONObject) tar_val.get(0)).get('description');

// input json :- {"role": "tester", "description": "do testing"}
// expected output :- "tester"

JSONObject tar_val = (JSONObject) property.get_current_value(entityId, entityAttributes);
String expected_output = (String) tar_val.get('role');

Property property = appConfigClient.getProperty("yaml-property");
if (property != null) {
    property.getPropertyDataType()     // STRING
    property.getPropertyDataFormat()   // YAML
    property.getCurrentValue(entityId, entityAttributes) // Yaml String is returned
    
}
```

</details>

## Set listener for feature or property data changes

The SDK provides mechanism to notify you in real-time when feature flag's or property's configuration changes. You can
subscribe to configuration changes using the same appConfigClient.

```java
appConfigClient.registerConfigurationUpdateListener(new ConfigurationUpdateListener() {
    @Override
    public void onConfigurationUpdate() {
        System.out.println("Received updated configurations");
        // **add your code**
        // To find the effect of any configuration changes, you can call the feature or property related methods
        
        // Feature feature = appConfigClient.getFeature("numeric-feature");
        // Integer newValue = (Integer) feature.getCurrentValue(entityId, entityAttributes);
    }
});
```

## Fetch latest data 

```java
appConfigClient.fetchConfigurations();
```

## Enable debugger (Optional)

```java
appConfigClient.enableDebug(true);
```

## License

This project is released under the Apache 2.0 license. The license's full text can be found in [LICENSE](https://github.com/IBM/appconfiguration-java-sdk/blob/master/LICENSE)
