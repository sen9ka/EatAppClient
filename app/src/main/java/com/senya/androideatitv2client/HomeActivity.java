package com.senya.androideatitv2client;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.senya.androideatitv2client.Common.Common;
import com.senya.androideatitv2client.Database.CartDataSource;
import com.senya.androideatitv2client.Database.CartDatabase;
import com.senya.androideatitv2client.Database.LocalCartDataSource;
import com.senya.androideatitv2client.EventBus.BestDealItemClick;
import com.senya.androideatitv2client.EventBus.CategoryClick;
import com.senya.androideatitv2client.EventBus.CounterCartEvent;
import com.senya.androideatitv2client.EventBus.FoodItemClick;
import com.senya.androideatitv2client.EventBus.HideFABCart;
import com.senya.androideatitv2client.EventBus.MenuInflateEvent;
import com.senya.androideatitv2client.EventBus.MenuItemBack;
import com.senya.androideatitv2client.EventBus.MenuItemEvent;
import com.senya.androideatitv2client.EventBus.PopularCategoryClick;
import com.senya.androideatitv2client.Model.CategoryModel;
import com.senya.androideatitv2client.Model.FoodModel;
import com.senya.androideatitv2client.Model.UserModel;
import com.senya.androideatitv2client.databinding.ActivityHomeBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityHomeBinding binding;
    private DrawerLayout drawer;
    private NavController navController;
    private NavigationView navigationView;
    private CartDataSource cartDataSource;
    android.app.AlertDialog dialog;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    int menuClickId=-1;

    @BindView(R.id.fab)
    CounterFab fab;

    @Override
    protected void onResume() {
        super.onResume();
        //countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initPlaceClient();

        dialog = new android.app.AlertDialog.Builder(this).setCancelable(false).create();

        ButterKnife.bind(this);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> navController.navigate(R.id.nav_cart));
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_restaurant,
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail,
                R.id.nav_view_orders, R.id.nav_cart, R.id.nav_food_list)
                .setOpenableLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();

        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = (TextView) headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hello, ", Common.currentUser.getName(),txt_user);

        //не видим корзину при выборе ресторанов
        EventBus.getDefault().postSticky(new HideFABCart(true));
    }

    private void initPlaceClient() {
        Places.initialize(this,getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_home);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        item.setChecked(true);
        drawer.closeDrawers();
        switch (item.getItemId()){
            case R.id.nav_restaurant:
                if(item.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_restaurant);
                break;
            case R.id.nav_home:
                if(item.getItemId() != menuClickId)
                {
                    navController.navigate(R.id.nav_home);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;
            case R.id.nav_menu:
                if(item.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_menu);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;
            case R.id.nav_cart:
                if(item.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_cart);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;
            case R.id.nav_view_orders:
                if(item.getItemId() != menuClickId) {
                    navController.navigate(R.id.nav_view_orders);
                    EventBus.getDefault().postSticky(new MenuInflateEvent(true));
                }
                break;
            case R.id.nav_sign_out:
                signOut();
                break;
            case R.id.nav_news:
                showSubscribeNews();
                break;
            case R.id.nav_update_info:
                showUpdateInfoDialog();
                break;
        }
        menuClickId = item.getItemId();
        return true;
    }

    private void showSubscribeNews() {
        Paper.init(this);
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("News System");
        builder.setMessage("Do you want to subscribe to notifications from us?");

        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_subscribe_news, null);
        CheckBox ckb_news = (CheckBox) itemView.findViewById(R.id.ckb_subscribe_news);
        boolean isSubscribeNews = Paper.book().read(Common.IS_SUBSCRIBE_NEWS,false);
        if(isSubscribeNews)
            ckb_news.setChecked(true);
        builder.setNegativeButton("CANCEL", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        }).setPositiveButton("SEND", (dialogInterface, i) -> {
            if(ckb_news.isChecked())
            {
                Paper.book().write(Common.IS_SUBSCRIBE_NEWS,true);
                FirebaseMessaging.getInstance()
                        .subscribeToTopic(Common.NEWS_TOPIC)
                        .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnSuccessListener(unused -> Toast.makeText(HomeActivity.this, "Successfully subscribed!", Toast.LENGTH_SHORT).show());
            }
            else
            {
                Paper.book().delete(Common.IS_SUBSCRIBE_NEWS);
                FirebaseMessaging.getInstance()
                        .unsubscribeFromTopic(Common.NEWS_TOPIC)
                        .addOnFailureListener(e -> Toast.makeText(HomeActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnSuccessListener(unused -> Toast.makeText(HomeActivity.this, "Successfully unsubscribed!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showUpdateInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Update Info");
        builder.setMessage("Please Fill Information");

        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        EditText edt_name= (EditText)itemView.findViewById(R.id.edt_name);
        TextView txt_address_detail = (TextView) itemView.findViewById(R.id.txt_address_detail);
        EditText edt_phone= (EditText)itemView.findViewById(R.id.edt_phone);

        places_fragment = (AutocompleteSupportFragment)getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                placeSelected = place;
                txt_address_detail.setText(place.getAddress());
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(HomeActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //set data
        edt_name.setText(Common.currentUser.getName());
        txt_address_detail.setText(Common.currentUser.getAddress());
        edt_phone.setText(Common.currentUser.getPhone());

        builder.setNegativeButton("CANCEL", (dialogInterface, which) -> dialogInterface.dismiss());

        builder.setPositiveButton("UPDATE", (dialogInterface, which) -> {
            if(placeSelected != null)
            {
                if(TextUtils.isEmpty(edt_name.getText().toString()))
                {
                    Toast.makeText(HomeActivity.this, "Please enter Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String,Object> update_data = new HashMap<>();
                update_data.put("name",edt_name.getText().toString());
                update_data.put("address",txt_address_detail.getText().toString());
                update_data.put("lat",placeSelected.getLatLng().latitude);
                update_data.put("lng",placeSelected.getLatLng().longitude);

                FirebaseDatabase.getInstance()
                        .getReference(Common.USER_REFERENCES)
                        .child(Common.currentUser.getUid())
                        .updateChildren(update_data)
                        .addOnFailureListener(e -> {
                            dialogInterface.dismiss();
                            Toast.makeText(HomeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnSuccessListener(aVoid -> {
                            dialogInterface.dismiss();
                            Toast.makeText(HomeActivity.this, "Information successfully updated", Toast.LENGTH_SHORT).show();
                            Common.currentUser.setName(update_data.get("name").toString());
                            Common.currentUser.setAddress(update_data.get("address").toString());
                            Common.currentUser.setLat(Double.parseDouble(update_data.get("lat").toString()));
                            Common.currentUser.setLng(Double.parseDouble(update_data.get("lng").toString()));
                        });
            }
            else
            {
                Toast.makeText(this, "Please select address", Toast.LENGTH_SHORT).show();
            }

        });

        builder.setView(itemView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialogInterface -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });
        dialog.show();
    }

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Do you really want to sign out?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Common.selectedFood = null;
                Common.categorySelected = null;
                Common.currentUser = null;
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(HomeActivity.this,MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //EventBus

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHideFABEvent(HideFABCart event)
    {
        if(event.isHidden())
        {
            fab.hide();
        }
        else
            fab.show();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategorySelected(CategoryClick event)
    {
        if(event.isSuccess())
        {
            navController.navigate(R.id.nav_food_list);
            //Toast.makeText(this, "Click to "+event.getCategoryModel().getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onFoodItemClick(FoodItemClick event)
    {
        if(event.isSuccess())
        {
            navController.navigate(R.id.nav_food_detail);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCartCounter(CounterCartEvent event)
    {
        //if(event.isSuccess())
        //{
            //countCartItem();
       // }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onBestDealItemClick(BestDealItemClick event)
    {
        if(event.getBestDealModel() != null)
        {

            dialog.show();

            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentRestaurant.getUid())
                    .child(Common.CATEGORY_REF)
                    .child(event.getBestDealModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists())
                            {
                                Common.categorySelected = snapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(snapshot.getKey());

                                //Load food item
                                FirebaseDatabase.getInstance()
                                        .getReference(Common.RESTAURANT_REF)
                                        .child(Common.currentRestaurant.getUid())
                                        .child(Common.CATEGORY_REF)
                                        .child(event.getBestDealModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getBestDealModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists())
                                                {
                                                    for(DataSnapshot itemSnapShot: snapshot.getChildren())
                                                    {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());
                                                    }
                                                    navController.navigate(R.id.nav_food_detail);
                                                }
                                                else
                                                {
                                                    Toast.makeText(HomeActivity.this, "Item does not exist", Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                            else
                            {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item does not exist", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPopularItemClick(PopularCategoryClick event)
    {
        if(event.getPopularCategoryModel() != null)
        {
            dialog.show();
            FirebaseDatabase.getInstance()
                    .getReference(Common.RESTAURANT_REF)
                    .child(Common.currentRestaurant.getUid())
                    .child(Common.CATEGORY_REF)
                    .child(event.getPopularCategoryModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists())
                            {
                                Common.categorySelected = snapshot.getValue(CategoryModel.class);
                                Common.categorySelected.setMenu_id(snapshot.getKey());

                                //Load food item
                                FirebaseDatabase.getInstance()
                                        .getReference(Common.RESTAURANT_REF)
                                        .child(Common.currentRestaurant.getUid())
                                        .child(Common.CATEGORY_REF)
                                        .child(event.getPopularCategoryModel().getMenu_id())
                                        .child("foods")
                                        .orderByChild("id")
                                        .equalTo(event.getPopularCategoryModel().getFood_id())
                                        .limitToLast(1)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                if(snapshot.exists())
                                                {
                                                    for(DataSnapshot itemSnapShot: snapshot.getChildren())
                                                    {
                                                        Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                        Common.selectedFood.setKey(itemSnapShot.getKey());
                                                    }
                                                    navController.navigate(R.id.nav_food_detail);
                                                }
                                                else
                                                {
                                                    Toast.makeText(HomeActivity.this, "Item does not exist", Toast.LENGTH_SHORT).show();
                                                }
                                                dialog.dismiss();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                dialog.dismiss();
                                                Toast.makeText(HomeActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                            else
                            {
                                dialog.dismiss();
                                Toast.makeText(HomeActivity.this, "Item does not exist", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void countCartItem() {
        cartDataSource.countItemInCart(Common.currentUser.getUid(),Common.currentRestaurant.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        fab.setCount(integer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty"))
                        {
                            Toast.makeText(HomeActivity.this, "[COUNT CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        else
                            fab.setCount(0);
                    }
                });
    }

    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    public void countCartAgain(CounterCartEvent event)
    {
        if(event.isSuccess())
            if(Common.currentRestaurant != null)
                countCartItem();
    }

   @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
   public void onMenuItemBack(MenuItemBack event)
   {
       menuClickId = -1;
       if(getSupportFragmentManager().getBackStackEntryCount() > 0)
           getSupportFragmentManager().popBackStack();
   }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onRestaurantClick(MenuItemEvent event)
    {
        Bundle bundle = new Bundle();
        bundle.putString("restaurant",event.getRestaurantModel().getUid());
        navController.navigate(R.id.nav_home,bundle);
        EventBus.getDefault().postSticky(new MenuInflateEvent(true));
        EventBus.getDefault().postSticky(new HideFABCart(false));
        countCartItem();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onInflateMenu(MenuInflateEvent event)
    {
        navigationView.getMenu().clear();
        if(event.isShowDetail())
            navigationView.inflateMenu(R.menu.restaurant_detail_menu);
        else
            navigationView.inflateMenu(R.menu.activity_home_drawer);

    }

}