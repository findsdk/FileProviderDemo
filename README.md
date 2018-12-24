FileProvider

    allprojects {
        repositories {
            jcenter()
            //add maven
            maven { url "http://10.200.180.48:8081/nexus/content/repositories/android-library" }
        }
    }


    dependencies {
        compile 'cn.com.carfree:fileprovider:1.0.0'
    }