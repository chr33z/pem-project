package de.mimuc.pem_music_graph;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.List;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.mimuc.pem_music_graph.favorite_list.FavoriteListAdapter;
import de.mimuc.pem_music_graph.favorite_list.FavoriteListListener;
import de.mimuc.pem_music_graph.favorite_list.FavoriteLocation;
import android.widget.TextView.OnEditorActionListener;
import de.mimuc.pem_music_graph.graph.GenreGraphConstants;
import de.mimuc.pem_music_graph.graph.GenreGraphListener;
import de.mimuc.pem_music_graph.graph.GenreNode;
import de.mimuc.pem_music_graph.graph.MusicGraphView;
import de.mimuc.pem_music_graph.list.Event;
import de.mimuc.pem_music_graph.list.EventControllerListener;
import de.mimuc.pem_music_graph.list.EventListAdapter;
import de.mimuc.pem_music_graph.list.EventController;
import de.mimuc.pem_music_graph.utils.ApiGuard;
import de.mimuc.pem_music_graph.utils.ApplicationController;
import de.mimuc.pem_music_graph.utils.FileUtils;
import de.mimuc.pem_music_graph.utils.JsonPreferences;
import de.mimuc.pem_music_graph.utils.UndoBarController;
import de.mimuc.pem_music_graph.utils.UndoBarController.UndoListener;

/**
 * Shows both the music graph and the location list in one place
 * 
 * @author Christopher Gebhardt, Anna Kienle, Nicole Lipppner, Edina Smajic
 * 
 */
public class ListActivity extends FragmentActivity implements
	ConnectionCallbacks, OnConnectionFailedListener,
	EventControllerListener, GenreGraphListener, FavoriteListListener,
	UndoListener {

	private static final String TAG = ListActivity.class.getSimpleName();

	// sliding panel
	private SlidingUpPanelLayout mSlideUpPanel;

	// graph
	private MusicGraphView mGraphView;

	// list
	private EventListAdapter mAdapter;
	private ExpandableListView mEventListView;

	private EventController mEventController;
	private Location mCurrentLocation;
	private Location mAlternativeLocation;

	private boolean mUseAlternativeLocation;

	// location services
	private LocationClient mLocationClient;

	// navigation drawer right
	private ExpandableListView mListFavorites;
	private UndoBarController mUndoBarController;

	// navigation drawer left
	private DatePicker mDatePicker;
	private Button mBtnOk;
	private Button mBtnReset;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;

	int mScreenWidth= 0;
	int mScreenHeight = 0;
	
	private SharedPreferences mSharedPreferences;

	// needed for redraw operations
	private View mRootView;
	private RelativeLayout mListContainer;
	
	private boolean mAttachMap = false;
	private Event mEventToAttach;
	
	private PemMapFragment mMapFragment;

	/**
	 * Is called when location updates arrive
	 */
	private LocationListener mLocationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			if (location == null)
				return;

			if (mCurrentLocation != null
					&& location.distanceTo(mCurrentLocation) < ApplicationController.MAX_UPDATE_DISTANCE) {
				Log.i(TAG, "Location not changed.");
				return;
			}

			Log.i(TAG, "Location changed to (" + location.getLatitude() + ", "
					+ location.getLongitude() + ")");

			mEventController.updateEvents(location);
		}
	};

//	public void onRadioButtonClicked(View view) {
//		// Is the button now checked?
//		boolean checked = ((RadioButton) view).isChecked();
//
//		// Check which radio button was clicked
//		switch (view.getId()) {
//		case R.id.radio_ownStart:
//			if (checked) {
//				mUseAlternativeLocation = false;
//				mEventController.useAlternativeLocation(false);
//				mEventController.setLocation(mCurrentLocation);
//				onEventControllerUpdate();
//			}
//			break;
//		case R.id.radio_otherStart:
//			if (checked) {
//				mUseAlternativeLocation = true;
//				if(mAlternativeLocation != null){
//					mEventController.useAlternativeLocation(true);
//					mEventController.setLocation(mAlternativeLocation);
//					onEventControllerUpdate();
//				}
//			}
//			break;
//		}
//	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_combined_view);

		mRootView = findViewById(R.id.root);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		mMapFragment = new PemMapFragment();

		mListFavorites = (ExpandableListView) findViewById(R.id.listFavorites);
		mListFavorites.setEmptyView(findViewById(R.id.favorite_empty));

		// set undo listener to undo favorite remove
		mUndoBarController = new UndoBarController(findViewById(R.id.undobar), this);

		// get location updates
		mLocationClient = new LocationClient(this, this, this);

		// init graph
		mGraphView = new MusicGraphView(this);
		mGraphView.setGenreGraphListener(this);
		((FrameLayout)findViewById(R.id.graph_view_frame)).addView(mGraphView);
		mGraphView.onThreadResume();

		// try to load a json file we got from start screen
		JSONObject eventList = FileUtils.readEventListFromStorage(this);
		if (eventList != null) {

			// initialize controller
			double latitude = getIntent().getDoubleExtra("latitude", 0.0);
			double longitude = getIntent().getDoubleExtra("longitude", 0.0);
			
			Location location = new Location("location");
			location.setLatitude(latitude);
			location.setLongitude(longitude);
			mCurrentLocation = location;
			mEventController = new EventController(this, eventList, location);
		} else {
			mEventController = new EventController(this);
		}

		mEventController.setGenreNode(mGraphView.getRootNode());

		String favorites = mSharedPreferences.getString("favorites", "");
		if (!(favorites.isEmpty())) {
			mEventController.setFavorites(JsonPreferences
					.createFavoritesFromJson(favorites));
			mEventController.updateFavorites();
			updateFavoriteList();
		}

		// intialize listview
		mListContainer = (RelativeLayout) findViewById(R.id.list_container);
		mEventListView = (ExpandableListView) findViewById(R.id.list_view);
		mEventListView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if(mAttachMap && mEventToAttach != null){
					attachMap(mEventToAttach);
					mAttachMap = false;
				}
			}
			
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
			}
		});
		
		mAdapter = new EventListAdapter(this, mEventController.getEventList());
		mEventListView.setAdapter(mAdapter);

		/*
		 * Force redraw of list while scrolling to prevent glitches
		 */
		if(ApiGuard.belowJellyBean()){
			mEventListView.setOnScrollListener(new OnScrollListener() {

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
				}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {

					mEventListView.bringToFront();
					mRootView.requestLayout();
					mRootView.invalidate();
				}
			});
		}

		updateFavoriteList();

		// initialize dimensions
		mScreenWidth = ApplicationController.getScreenWidth();
		mScreenHeight = ApplicationController.getScreenHeight();

		// initialize slide panel
		mSlideUpPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
		mSlideUpPanel.setPanelHeight((int) (mScreenHeight * 0.5));
		mSlideUpPanel.setDragView(findViewById(R.id.list_handle));
		mSlideUpPanel.setCoveredFadeColor(getResources().getColor(android.R.color.transparent));
		mSlideUpPanel.setPanelSlideListener(new PanelSlideListener() {

			@Override
			public void onPanelSlide(View panel, float slideOffset) {
				/*
				 * Force redraw of list while panel sliding to prevent glitches
				 */
				if(ApiGuard.apiBelow(Build.VERSION_CODES.JELLY_BEAN)){
					mListContainer.bringToFront();
					mRootView.requestLayout();
					mRootView.invalidate();
				}

				mEventListView.setPadding(
						mEventListView.getPaddingLeft(), 
						mEventListView.getPaddingTop(), 
						mEventListView.getPaddingRight(), 
						findViewById(R.id.list_container).getTop());
			}

			@Override
			public void onPanelExpanded(View panel) {
				if(ApiGuard.belowJellyBean()){
					//					FrameLayout graphContainer = (FrameLayout) rootView.findViewById(R.id.graph_view_frame);
					//					for (int i = 0; i < slideUpPanel.getChildCount(); i++) {
					//						View child = slideUpPanel.getChildAt(i);
					//						if(child.getId() == R.id.graph_view_frame){
					//							slideUpPanel.removeViewAt(i);
					//						}
					//					}
					//					slideUpPanel.addView(graphContainer);
					//					graphView.onThreadResume();
				}
			}

			@Override
			public void onPanelCollapsed(View panel) {
			}

			@Override
			public void onPanelAnchored(View panel) {
			}
		});

		// get result from edit text
		final RadioButton radioBtnOtherLocation = (RadioButton) findViewById(R.id.radio_otherStart);
		final RadioButton radioBtnOwnLocation = (RadioButton) findViewById(R.id.radio_ownStart);
		final EditText editText = (EditText) findViewById(R.id.auto_text);
		editText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEND) {
					getLocationFromAddress(editText.getText().toString());
					handled = true;
				}
				return handled;
			}

		});

		editText.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					radioBtnOtherLocation.setChecked(true);
				}
			}
		});

		mDatePicker = (DatePicker) this.findViewById(R.id.datePicker1);
		mBtnOk = (Button) this.findViewById(R.id.ok_button);
		mBtnReset = (Button) this.findViewById(R.id.reset_button);
		datePicker(null, null);

		mDrawerLayout = (DrawerLayout) this.findViewById(R.id.drawer_layout);

		if(ApiGuard.belowJellyBean()){
			mDrawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
		}

		/*
		 * Force redraw of drawer to prevent glitches
		 */
		if(ApiGuard.belowJellyBean()){
			mDrawerLayout.setDrawerListener(new DrawerListener() {

				@Override
				public void onDrawerStateChanged(int arg0) { }

				@Override
				public void onDrawerSlide(View arg0, float arg1) {
					mDrawerLayout.bringToFront();
					mRootView.requestLayout();
					mRootView.invalidate();
				}

				@Override
				public void onDrawerOpened(View arg0) { }

				@Override
				public void onDrawerClosed(View arg0) { }
			});
		}

		//		drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
		//				R.drawable.ic_drawer, R.string.drawer_open,
		//				R.string.drawer_close) {
		//
		//			/** Called when a drawer has settled in a completely closed state. */
		//			public void onDrawerClosed(View view) {
		//				super.onDrawerClosed(view);
		//				// getActionBar().setTitle(mTitle);
		//				// invalidateOptionsMenu(); // creates call to
		//				// onPrepareOptionsMenu()
		//				graphView.onThreadResume();
		//			}
		//
		//			/** Called when a drawer has settled in a completely open state. */
		//			public void onDrawerOpened(View drawerView) {
		//				super.onDrawerOpened(drawerView);
		//				// getActionBar().setTitle(mDrawerTitle);
		//				// invalidateOptionsMenu(); // creates call to
		//				// onPrepareOptionsMenu()
		//				graphView.onThreadPause();
		//			}
		//		};

		// Set the drawer toggle as the DrawerListener
		//		drawerLayout.setDrawerListener(drawerToggle);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
	}
	
	

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			mDrawerLayout.closeDrawer(Gravity.RIGHT);

			if (!mDrawerLayout.isDrawerOpen(Gravity.LEFT))
				mDrawerLayout.openDrawer(Gravity.LEFT);
			else
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			return true;

		case R.id.favorites:
			mDrawerLayout.closeDrawer(Gravity.LEFT);

			if (!mDrawerLayout.isDrawerOpen(Gravity.RIGHT))
				mDrawerLayout.openDrawer(Gravity.RIGHT);
			else
				mDrawerLayout.closeDrawer(Gravity.RIGHT);
			return true;

		default:
			return super.onOptionsItemSelected(item);

		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.combined_view, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mLocationClient != null)
			mLocationClient.connect();

		if(mGraphView != null)
			mGraphView.onThreadResume();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(mGraphView != null)
			mGraphView.onThreadPause();

		if (mLocationClient != null)
			mLocationClient.disconnect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		String json = mEventController.getJsonForSharedPreferences();
		mSharedPreferences.edit().putString("events", json).commit();
		String favorites = JsonPreferences
				.createJsonFromFavorites(mEventController.getFavorites());
		mSharedPreferences.edit().putString("favorites", favorites).commit();

		if (mGraphView != null)
			mGraphView.onThreadPause();

		if (mLocationClient != null)
			mLocationClient.disconnect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {

	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.i(TAG, "Location Connected.");
		// Get last known location
		Location lastLocation = mLocationClient.getLastLocation();
		mLocationListener.onLocationChanged(lastLocation);

		// Create location request
		LocationRequest locationRequest = LocationRequest
				.create()
				.setInterval(
						ApplicationController.DEFAULT_UPDATE_LOCATION_INTERVAL)
						.setExpirationDuration(
								ApplicationController.DEFAULT_TERMINATE_SAT_FINDING)
								.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

		mLocationClient.requestLocationUpdates(locationRequest,
				mLocationListener);
	}

	@Override
	public void onDisconnected() {

	}

	@Override
	public void onEventControllerUpdate() {
		Log.d(TAG, "Update ListAdapter");

		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				// save last scroll position
				int index = mEventListView.getFirstVisiblePosition();
				View v = mEventListView.getChildAt(0);
				int top = (v == null) ? 0 : v.getTop();

				mAdapter = new EventListAdapter(ListActivity.this, mEventController
						.getEventList());
				mEventListView.setAdapter(mAdapter);
				if (mEventController.isNoEvents()) {
					mAdapter.setNoEvents(true);
					mEventController.setNoEvents(false);
				}

				// restore scroll position
				mEventListView.setSelectionFromTop(index, top);
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		// hijack back button to do what we want
		if (keyCode == KeyEvent.KEYCODE_BACK) {

			if (!mSlideUpPanel.isExpanded() && !mGraphView.isAtRoot()) {
				mGraphView.graphNavigateBack();
				return true;
			} else if (mSlideUpPanel.isExpanded()) {
				mSlideUpPanel.collapsePane();
				return true;
			} else {
				moveTaskToBack(true);
				return true;
			}
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (!mDrawerLayout.isDrawerOpen(Gravity.LEFT))
				mDrawerLayout.openDrawer(Gravity.LEFT);
			else
				mDrawerLayout.closeDrawer(Gravity.LEFT);
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * calls the method to add a favorite in the EventController
	 */
	@Override
	public void onAddFavorites(String locationID) {
		mEventController.addFavorite(locationID);
		mAdapter.notifyDataSetChanged();
		updateFavoriteList();
	}

	/**
	 * calls the method to remove the ID from favorites in the EventController
	 */
	@Override
	public void onRemoveFavorites(String locationID) {
		mEventController.removeFavorite(locationID);
		mAdapter.notifyDataSetChanged();
		updateFavoriteList();
	}

	@Override
	public void onExpandItem(int ID) {
		mEventController.expandItem(ID);
		mAdapter.notifyDataSetChanged();

		SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		if (fragment != null)
			getSupportFragmentManager().beginTransaction().remove(fragment)
			.commit();
	}

	@Override
	public void onCollapseItem() {
		mEventController.collapseItem();
		mAdapter.notifyDataSetChanged();

		SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		if (fragment != null)
			getSupportFragmentManager().beginTransaction().remove(fragment)
			.commit();

//		if(ApiGuard.apiBelow(Build.VERSION_CODES.JELLY_BEAN)){
//			locationListView.findViewById(R.id.map_container).setVisibility(View.GONE);
//		}
	}

	@Override
	public void onGraphUpdate(GenreNode node, int newHeight) {
		mEventController.setGenreNode(node);
		onEventControllerUpdate();
		mSlideUpPanel.animatePanelHeight(mRootView.getMeasuredHeight(), (int) (newHeight + GenreGraphConstants.SCREEN_MARGIN_FACTOR * mScreenWidth * 3));
	}

	@Override
	public void onShareEvent(Event event) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TITLE, event.eventName);
		intent.putExtra(Intent.EXTRA_TEXT, "Ich gehe heute Abend zu "
				+ event.eventName + " ins " + event.locationName + ". Lust? (;");
		startActivity(Intent.createChooser(intent, "Share with..."));
	}

	@Override
	public void scrollEventTop(View listItem) {
		if (listItem != null) {
			mEventListView.smoothScrollToPositionFromTop(
					(Integer) listItem.getTag(), 0);
		}
	}

	@Override
	public void attachMap(Event event) {
		if (event != null) {
			SupportMapFragment fragment = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map);
			if (fragment != null)
				getSupportFragmentManager().beginTransaction().remove(fragment)
				.commit();
			
			if(mMapFragment == null){
				Bundle args = new Bundle();
				args.putDouble("lat", Double.parseDouble(event.locationLatitude));
				args.putDouble("lon", Double.parseDouble(event.locationLongitude));
				mMapFragment = new PemMapFragment();
				mMapFragment.setArguments(args);
			} else {
				mMapFragment.setLocation(new LatLng(
						Double.parseDouble(event.locationLatitude), 
						Double.parseDouble(event.locationLongitude)));
			}
			
			try {
				getSupportFragmentManager().beginTransaction()
					.replace(R.id.map_container, mMapFragment).commit();
			} catch(Exception e){
				Log.e(TAG, "attachMap | could not find layout id \"map_container\"!");
			}
			
		}
	}

	@Override
	public void onEventControllerFinished() {
		updateFavoriteList();
	}

	/**
	 * Update Favorite List and attach next event to favorite
	 */
	private void updateFavoriteList() {
		LinkedList<FavoriteLocation> favLocations = new LinkedList<FavoriteLocation>();
		List<Event> eventList = mEventController.getCompleteEventList();

		/*
		 * iterate over all favorites and all events if we find an event that
		 * takes place earlirer, save it we want to have the events for the
		 * location that takes place next
		 */
		for (Entry<String, FavoriteLocation> entry : mEventController
				.getFavorites().entrySet()) {
			Event nextEvent = null;

			for (Event event : eventList) {
				String favoriteId = entry.getKey();

				if (event.locationID.equals(favoriteId)) {
					if (nextEvent == null) {
						nextEvent = event;
					} else {
						if (Long.parseLong(nextEvent.startTime) >= Long
								.parseLong(event.startTime)) {
							nextEvent = event;
						}
					}
				}
			}
			FavoriteLocation favLocation = entry.getValue();
			favLocation.setNextEvent(nextEvent);
			favLocations.add(favLocation);
		}

		mListFavorites.setAdapter(new FavoriteListAdapter(this,
				favLocations, this));
	}

	@Override
	public void onFavoriteDelete(final String favoriteId) {

		// Dialog for ICE CREAM SANDWICH
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(R.string.dialog_title_delete_favorite)
			.setPositiveButton(R.string.dialog_positiv_delete_favorite,
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					onRemoveFavorites(favoriteId);
				}

			})
			.setNegativeButton(
					R.string.dialog_negative_delete_favorite, null)
					.show();

		} else {
			Bundle restoreToken = new Bundle();
			restoreToken.putString("id", favoriteId);

			mUndoBarController.showUndoBar(false,
					getString(R.string.undobar_sample_message), restoreToken);

			onRemoveFavorites(favoriteId);

			Log.d(TAG, "Removed favorite from list");
		}
	}

	@Override
	public void onUndo(Bundle token) {
		if (token != null) {
			String favoriteId = token.getString("id");
			onAddFavorites(favoriteId);
			updateFavoriteList();

			Log.d(TAG, "Remove favorite from list undo");
		}
	}

	/**
	 * set the color of the date picker dividers
	 * 
	 * @param listener
	 * @param calendar
	 * @return
	 */
	public DatePicker datePicker(OnDateSetListener listener, Calendar calendar) {
		mBtnOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.v(TAG,
						mDatePicker.getDayOfMonth() + " "
								+ (mDatePicker.getMonth() + 1) + " "
								+ mDatePicker.getYear());
				String dateTime = mDatePicker.getYear() + "-"
						+ (mDatePicker.getMonth() + 1) + "-"
						+ mDatePicker.getDayOfMonth() + "T" + "00" + ":" + "00"
						+ ":00.000";
				DateTime time = DateTime.parse(dateTime);
				mEventController.setDateTime(time);
				mEventController.useAlternativeTime(true);
				onEventControllerUpdate();
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
		});

		mBtnReset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mEventController.useAlternativeTime(false);
				onEventControllerUpdate();
				mDrawerLayout.closeDrawer(Gravity.LEFT);
			}
		});

		LinearLayout llFirst = (LinearLayout) mDatePicker.getChildAt(0);
		LinearLayout llSecond = (LinearLayout) llFirst.getChildAt(0);
		for (int i = 0; i < llSecond.getChildCount(); i++) {
			NumberPicker picker = (NumberPicker) llSecond.getChildAt(i); // Numberpickers
			Field[] pickerFields = NumberPicker.class.getDeclaredFields();
			for (Field pf : pickerFields) {
				if (pf.getName().equals("mSelectionDivider")) {
					pf.setAccessible(true);
					try {
						pf.set(picker,
								getResources().getDrawable(
										R.drawable.date_picker_shape));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (NotFoundException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					break;
				}
			}
		}

		return mDatePicker;
	}
	
	@Override
	public void onFavoriteClick(FavoriteLocation favoriteLocation) {
		Event event = null;
		int position = 0;
		
		List<Event> eventList = mEventController.getEventList();

		if(mAdapter != null && favoriteLocation.nextEvent != null){
			for (int i = 0; i < eventList.size(); i++) {
//				Event iEvent = (Event) adapter.getGroup(i);
				Event iEvent = eventList.get(i);

				if(iEvent.ID == favoriteLocation.nextEvent.ID){
					event = iEvent;
					position = i;
					break;
				}
			}

			if(event != null){
				for (int i = 0; i < eventList.size(); i++) {
					mEventListView.collapseGroup(i);
					eventList.get(i).isExpanded = false;
				}
				
				mDrawerLayout.closeDrawer(findViewById(R.id.right_drawer));
				mSlideUpPanel.expandPane();
				mEventListView.smoothScrollToPositionFromTop(position, 0, 0);
				
				event.isExpanded = true;
				onExpandItem(event.ID);
				mEventListView.expandGroup(position);
				
				final Event attachEvent = event;
				
				Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						attachMap(attachEvent);
					}
				}, 750);
			}
		}
	}
	
	private void getLocationFromAddress(String address){
		address = address.replace(", ", " ");
		address = address.replace(",", " ");
		address = address.replace(" ", "+");
		address = address.replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        String url = "http://maps.googleapis.com/maps/api/geocode/json?address="+ address +"&sensor=true";
		
		// POST request
		JsonObjectRequest req = new JsonObjectRequest(url, null, 
				
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.i(TAG, "...success!");
						onAlternativeLocationsReceived(response);
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						if (error.getMessage() == null) {
							Log.e(TAG,
									"...could not retrieve locations");
							return;
						}
						VolleyLog.e("Error: ", error.getMessage());
						Log.w(TAG, "...could not retrieve locations!");
					}
				});
		ApplicationController.getInstance().addToRequestQueue(req);
	}
	
	private void onAlternativeLocationsReceived(JSONObject result){
		Location closestLocation = null;
		float closestDistance = Float.MAX_VALUE;
		
		try {
			JSONArray results = result.getJSONArray("results");

			for (int i = 0; i < results.length(); i++) {
				JSONObject addressNode = results.getJSONObject(i);
				
				if(addressNode.has("geometry")){
					JSONObject locationNode = addressNode
							.getJSONObject("geometry").getJSONObject("location");
					
					Double lat = Double.parseDouble(locationNode.getString("lat"));
					Double lng = Double.parseDouble(locationNode.getString("lng"));
					
					Location location = new Location("location");
					location.setLatitude(lat);
					location.setLongitude(lng);
					
					// look for address, closest to our location
					
					if (closestLocation == null) {
						closestLocation = location;
					} else {
						float[] tmp = new float[1];
						Location.distanceBetween(
								mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 
								location.getLatitude(), location.getLongitude(), 
								tmp);
						
						float distance = tmp[0];
						if (distance < closestDistance) {
							closestLocation = location;
							closestDistance = distance;
						}
					}
				}
			}
		} catch (JSONException error) {
			Log.e(TAG, error.getMessage());
		}
		
		if(closestLocation != null){
			mAlternativeLocation = closestLocation;
			mEventController.setLocation(mAlternativeLocation);
			mUseAlternativeLocation = true;
			mEventController.useAlternativeLocation(true);
			onEventControllerUpdate();
			
			mDrawerLayout.closeDrawer(Gravity.LEFT);
			InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE); 

			inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                       InputMethodManager.HIDE_NOT_ALWAYS);
			
			Toast.makeText(this, "Neue Adresse gefunden.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(this, "Es konnte keine Addresse gefunden werden", Toast.LENGTH_LONG).show();
		}
	}
}
