Android Draggable SplitLayout
===
[![](https://jitpack.io/v/PhilTdr/SplitLayout.svg)](https://jitpack.io/#PhilTdr/SplitLayout)

An Android layout that splits the content between **two** child views. An optional draggable bar separates the child views, allowing the user to rearrange the space assigned to each view.

Gradle Setup
===
``` gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.PhilTdr:SplitLayout:v1.0.0'
}
```

How to use
===
``` xml
<de.of14.dev.splitlayout.SplitLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    app:minChildSize="100dp"
    app:splitterBackground="#099DEC"
    app:splitterDraggingBackground="#70BEE8"
    app:splitterIsDraggable="true"
    app:splitterPosition="0.4"
    app:splitterSize="16dp"
    app:splitterTouchAreaTolerance="16dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:text="@string/demo_text" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:text="@string/demo_text" />
</de.of14.dev.splitlayout.SplitLayout>
```
