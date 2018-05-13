package ch.mitto.missito.ui.tabs.chat.message.outgoing;

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

import java.util.EnumMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.mitto.missito.db.model.attach.LocationAttachRec;
import ch.mitto.missito.net.model.OutgoingMessageStatus;
import ch.mitto.missito.ui.tabs.chat.adapter.ChatMessage;
import ch.mitto.missito.ui.tabs.chat.adapter.OutgoingChatMessage;
import ch.mitto.missito.ui.tabs.chat.message.CellActionListener;
import ch.mitto.missito.ui.tabs.chat.message.ChatCellHelper;
import ch.mitto.missito.ui.tabs.chat.view.RoundedFrameLayout;
import ch.mitto.missito.util.Helper;

public class OutgoingLocationMessageViewHolder extends OutgoingMessageViewHolder implements OnMapReadyCallback {
    private static EnumMap<OutgoingMessageStatus, Integer> statusImageResource = new EnumMap<>(OutgoingMessageStatus.class);

    static {
        statusImageResource.put(OutgoingMessageStatus.RECEIVED, R.drawable.ic_chat_sent);
        statusImageResource.put(OutgoingMessageStatus.SEEN, R.drawable.ic_seen);
    }

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

    private GoogleMap googleMap;
    private LocationAttachRec location;
    private Marker marker;

    public OutgoingLocationMessageViewHolder(View itemView, CellActionListener listener) {
        super(itemView);
        this.listener = listener;
        ButterKnife.bind(this, itemView);
        mapView.onCreate(null);
        mapView.getMapAsync(this);
    }

    public void setMessage(OutgoingChatMessage message) {
        super.setMessage(message);
        location = message.attachmentRec.locations.get(0);
        locationName.setText(Helper.getTitle(location)); //Considering that message text is always empty. If message also have text, we just ignore it.Î

        if (googleMap != null) {
            changeMarkerPosition();
        }
    }

    @Override
    public void roundCorners(ChatMessage message) {
        bubbleRounded.setCornerRadii(getCornerRadiiFor(message));
    }

    private void changeMarkerPosition() {
        LatLng latLng = new LatLng(location.lat, location.lon);
        LatLngBounds bounds = getLatLngBounds(latLng);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0));

        marker.setPosition(latLng);
        marker.setTitle(Helper.getTitle(location));
        mapView.onResume();
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
