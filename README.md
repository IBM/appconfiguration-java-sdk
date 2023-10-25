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
    <version>0.3.5</version>
</dependency>
```

### Gradle

```sh
implementation group: 'com.ibm.cloud', name: 'appconfiguration-java-sdk', version: '0.3.5'
```

## Import the SDK

```java
import com.ibm.cloud.appconfiguration.sdk.AppConfiguration;
```

## Initialize SDK

```java
String region = AppConfiguration.REGION_US_SOUTH;
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
appConfigClient. The appConfigClient, once initialised, can be obtained across classes.
using **`AppConfiguration.getInstance()`**.  [See this example below](#fetching-the-appConfigClient-across-other-classes).

- region : Region name where the service instance is created. Use
    - `AppConfiguration.REGION_US_SOUTH` for Dallas
    - `AppConfiguration.REGION_EU_GB` for London
    - `AppConfiguration.REGION_AU_SYD` for Sydney
    - `AppConfiguration.REGION_US_EAST` for Washington DC
- guid : GUID of the App Configuration service. Get it from the service instance credentials section of the dashboard
- apikey : ApiKey of the App Configuration service. Get it from the service instance credentials section of the dashboard
- collectionId : Id of the collection created in App Configuration service instance under the **Collections** section.
- environmentId : Id of the environment created in App Configuration service instance under the **Environments** section.

### Connect using private network connection (optional)

Set the SDK to connect to App Configuration service by using a private endpoint that is accessible only through the IBM
Cloud private network.

```java
appConfigClient.usePrivateEndpoint(true);
```

This must be done before calling the `init` function on the SDK.

### (Optional)
In order for your application and SDK to continue its operations even during the unlikely scenario of App Configuration service across your application restarts, you can configure the SDK to work using a persistent cache. The SDK uses the persistent cache to store the App Configuration data that will be available across your application restarts.

```java
// 1. default (without persistent cache)
    appConfigClient.setContext(collectionId, environmentId);

// 2. optional (with persistent cache)
    ConfigurationOptions configOptions = new ConfigurationOptions();
    configOptions.setPersistentCacheDirectory("/var/lib/docker/volumes/");
    appConfigClient.setContext(collectionId, environmentId, configOptions);

```
- persistentCacheDirectory: Absolute path to a directory which has read & write permission for the user. The SDK will create a file - appconfiguration.json in the specified directory, and it will be used as the persistent cache to store the App Configuration service information.

When persistent cache is enabled, the SDK will keep the last known good configuration at the persistent cache. In the case of App Configuration server being unreachable, the latest configurations at the persistent cache is loaded to the application to continue working.

Please ensure that the cache file is not lost or deleted in any case. For example, consider the case when a kubernetes pod is restarted and the cache file (appconfiguration.json) was stored in ephemeral volume of the pod. As pod gets restarted, kubernetes destroys the ephermal volume in the pod, as a result the cache file gets deleted. So, make sure that the cache file created by the SDK is always stored in persistent volume by providing the correct absolute path of the persistent directory.


### (Optional)
The SDK is also designed to serve configurations, and perform feature flag & property evaluations without being connected to App Configuration service.

```java
ConfigurationOptions configOptions = new ConfigurationOptions();
configOptions.setBootstrapFile("saflights/flights.json");
configOptions.setLiveConfigUpdateEnabled(false);
appConfigClient.setContext(collectionId, environmentId, configOptions);
```

- bootstrapFile: Absolute path of the JSON file, which contains configuration details. Make sure to provide a proper JSON file. You can generate this file using `ibmcloud ac config` command of the IBM Cloud App Configuration CLI.
- liveConfigUpdateEnabled: Live configuration update from the server. Set this value to `false` if the new configuration values must not be fetched from the server. By default, this value is set to `true`.


## Get single feature

```java
Feature feature = appConfigClient.getFeature("online-check-in");

if (feature != null) {
    System.out.println("Feature Name : " + feature.getFeatureName());
    System.out.println("Feature Id : " + feature.getFeatureId());
    System.out.println("Feature Type : " + feature.getFeatureDataType());
    System.out.println("Is feature enabled? : " + feature.isEnabled());
}
```

## Get all features

```java
HashMap<String, Feature> features = appConfigClient.getFeatures();
```

## Evaluate a feature

Use the feature.getCurrentValue(entityId, entityAttributes) method to evaluate the value of the feature flag. This
method returns one of the Enabled/Disabled/Overridden value based on the evaluation.

```java
String entityId = "john_doe";
JSONObject entityAttributes = new JSONObject();
entityAttributes.put("city", "Bangalore");
entityAttributes.put("country", "India");

String value = (String) feature.getCurrentValue(entityId, entityAttributes);
```

* entityId: Id of the Entity. This will be a string identifier related to the Entity against which the feature is
  evaluated. For example, an entity might be an instance of an app that runs on a mobile device, a microservice that
  runs on the cloud, or a component of infrastructure that runs that microservice. For any entity to interact with App
  Configuration, it must provide a unique entity ID.
* entityAttributes: A JSON object consisting of the attribute name and their values that defines the specified entity.
  This is an optional parameter if the feature flag is not configured with any targeting definition. If the targeting is
  configured, then entityAttributes should be provided for the rule evaluation. An attribute is a parameter that is used
  to define a segment. The SDK uses the attribute values to determine if the specified entity satisfies the targeting
  rules, and returns the appropriate feature flag value.

## Get single property

```java
Property property = appConfigClient.getProperty("check-in-charges");

if (property != null) {
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

Use the property.getCurrentValue(entityId, entityAttributes) method to evaluate the value of the property. This method
returns the default property value or its overridden value based on the evaluation.

```java
String entityId = "john_doe";
JSONObject entityAttributes = new JSONObject();
entityAttributes.put("city", "Bangalore");
entityAttributes.put("country", "India");

String value = (String) property.getCurrentValue(entityId, entityAttributes);
```
* entityId: Id of the Entity. This will be a string identifier related to the Entity against which the property is
  evaluated. For example, an entity might be an instance of an app that runs on a mobile device, a microservice that
  runs on the cloud, or a component of infrastructure that runs that microservice. For any entity to interact with App
  Configuration, it must provide a unique entity ID.
* entityAttributes: A JSON object consisting of the attribute name and their values that defines the specified entity.
  This is an optional parameter if the property is not configured with any targeting definition. If the targeting is
  configured, then entityAttributes should be provided for the rule evaluation. An attribute is a parameter that is used
  to define a segment. The SDK uses the attribute values to determine if the specified entity satisfies the targeting
  rules, and returns the appropriate property value.

## Fetching the appConfigClient across other classes

Once the SDK is initialized, the appConfigClient can be obtained across other classes as shown below:

```java
// **other classes**

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
    property.getPropertyDataType();     // STRING
    property.getPropertyDataFormat();   // JSON
    property.getCurrentValue(entityId, entityAttributes); // JSONObject or JSONArray is returned

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
    property.getPropertyDataType();     // STRING
    property.getPropertyDataFormat();   // YAML
    property.getCurrentValue(entityId, entityAttributes); // Yaml String is returned

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
