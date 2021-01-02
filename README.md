Android Draggable
===
An Android layout that splits the content between **two** child views. An optional draggable bar separates the child views, allowing the user to rearrange the space assigned to each view.

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
