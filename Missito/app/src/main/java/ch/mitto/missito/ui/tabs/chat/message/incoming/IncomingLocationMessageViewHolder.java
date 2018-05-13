package ch.mitto.missito.ui.tabs.chat.message.incoming;

import android.ch.mitto.missito.R;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.IncomingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.CellActionListener;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.ui.tabs.chat.view.RoundedFrameLayout;
import ch.mitto.missito.util.Helper;

public class IncomingLocationMessageViewHolder extends IncomingMessageViewHolder implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private LocationAttachRec location;
    private Marker marker;

    private CellActionListener listener;

    @BindView(R.id.bubble)
    RoundedFrameLayout bubbleRounded;

    @BindView(R.id.map)
    MapView mapView;

    @BindView(R.id.location_name)
    TextView locationName;

    @OnClick(R.id.img_forward)
    public void onForwardPressed() {
        if (listener != null) {
            listener.onForwardSelected(message.id);
        }
    }

    @OnClick(R.id.dots_menu)
    public void onDotsMenuPressed() {
        ChatCellHelper.locationMessageAlertDialog(context, marker).show();
    }

    public IncomingLocationMessageViewHolder(View itemView, CellActionListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    public void setMessage(IncomingChatMessage message) {
        super.setMessage(message);
        location = message.attachmentRec.locations.get(0);
        locationName.setText(Helper.getTitle(location)); //Considering that message text is always empty. If message also have text, we just ignore it.
        if (googleMap != null) {
            updateMarker();
        }
    }

    @Override
    public void roundCorners(ChatMessage message) {
        bubbleRounded.setCornerRadii(getCornerRadiiFor(message));
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.googleMap = googleMap;
        final LatLng latLng = new LatLng(location.lat, location.lon);
        marker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(Helper.getTitle(location)));
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                openGoogleMaps();
            }
        });
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                LatLngBounds bounds = getLatLngBounds(latLng);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));
                mapView.onResume();
            }
        });
    }

    private void updateMarker() {
        LatLng latLng = new LatLng(location.lat, location.lon);
        LatLngBounds bounds = getLatLngBounds(latLng);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));

        marker.setPosition(latLng);
        marker.setTitle(Helper.getTitle(location));
        mapView.onResume();
    }

    @NonNull
    private LatLngBounds getLatLngBounds(LatLng latLng) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(SphericalUtil.computeOffset(latLng, location.radius, 90));
        boundsBuilder.include(SphericalUtil.computeOffset(latLng, location.radius, 270));
        return boundsBuilder.build();
    }

    private void openGoogleMaps() {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?z=17&q=%f,%f", location.lat, location.lon, location.lat, location.lon);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        context.startActivity(intent);
    }
}
