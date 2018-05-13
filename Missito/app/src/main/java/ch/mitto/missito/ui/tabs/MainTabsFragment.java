package ch.mitto.missito.ui.tabs;

import android.app.Activity;
import android.ch.mitto.missito.R;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.mitto.missito.ui.MainActivityAccess;
import ch.mitto.missito.ui.tabs.chats.ChatsFragment;
import ch.mitto.missito.ui.tabs.contacts.ContactListFragment;
import ch.mitto.missito.ui.tabs.settings.SettingsFragment;
import ch.mitto.missito.util.RealmDBHelper;

public class MainTabsFragment extends Fragment implements BottomNavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.bottom_navigation)
    BottomNavigationView bottomNavigation;

    @BindView(R.id.content)
    ViewPager viewPager;

    private MainActivityAccess listener;
    private ArrayList<Fragment> fragments;
    private ArrayList<Integer> menuItemsIds;

    public static MainTabsFragment newInstance() {
        return new MainTabsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragments = new ArrayList<>();
        fragments.add(ChatsFragment.newInstance());
        fragments.add(ContactListFragment.newInstance());
        fragments.add(SettingsFragment.newInstance());

        menuItemsIds = new ArrayList<>();
        menuItemsIds.add(R.id.action_chats);
        menuItemsIds.add(R.id.action_contacts);
        menuItemsIds.add(R.id.action_settings);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_tabs_fragment, container, false);
        listener.updateTitle(getString(R.string.app_name), false);

        ButterKnife.bind(this, view);
        bottomNavigation.setOnNavigationItemSelectedListener(this);

        PagerAdapter pagerAdapter =
                new PagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(pagerAdapter);


        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                bottomNavigation.getMenu()
                        .findItem(menuItemsIds.get(position))
                        .setChecked(true);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        if (!RealmDBHelper.existsChats()) {
            viewPager.setCurrentItem(1); //show contacts
        }
        return view;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        viewPager.setCurrentItem(menuItemsIds.indexOf(item.getItemId()));
        return true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (MainActivityAccess) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainActivityAccess");
        }
    }

    public class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

}
