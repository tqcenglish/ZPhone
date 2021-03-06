
package com.zycoo.android.zphone.ui.contacts;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zycoo.android.zphone.Engine;
import com.zycoo.android.zphone.R;
import com.zycoo.android.zphone.ZphoneApplication;
import com.zycoo.android.zphone.ui.dialpad.ScreenAV;
import com.zycoo.android.zphone.utils.Utils;
import com.zycoo.android.zphone.widget.SuperAwesomeCardFragment;

import org.doubango.ngn.media.NgnMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ContactDetailFragment extends SuperAwesomeCardFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public final Logger logger;
    private Uri mContactUri;
    // Defines a tag for identifying log entries
    private static final String TAG = ContactDetailFragment.class.getCanonicalName();
    public static final String EXTRA_CONTACT_URI =
            "com.zycoo.android.zphone.ui.contacts.EXTRA_CONTACT_URI";
    // The geo Uri scheme prefix, used with Intent.ACTION_VIEW to form a geographical address
    // intent that will trigger available apps to handle viewing a location (such as Maps)
    private static final String GEO_URI_SCHEME_PREFIX = "geo:0,0?q=";
    // Used to store references to key views, layouts and menu items as these need to be updated
    // in multiple methods throughout this class.
    private LinearLayout mDetailLayout;
    private List<String> mNumbers = new ArrayList<String>();

    public static ContactDetailFragment newInstance(int position, Uri contactUri) {
        // Create new instance of this fragment
        final ContactDetailFragment fragment = new ContactDetailFragment();
        // Create and populate the args bundle
        final Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        args.putParcelable(EXTRA_CONTACT_URI, contactUri);
        // Assign the args bundle to the new fragment
        fragment.setArguments(args);
        // Return fragment
        return fragment;
    }

    /**
     * Fragments require an empty constructor.
     */
    public ContactDetailFragment() {
        logger = LoggerFactory.getLogger(ContactDetailFragment.class.getCanonicalName());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If not being created from a previous state
        if (savedInstanceState == null) {
            // Sets the argument extra as the currently displayed contact
            setContact(getArguments() != null ?
                    (Uri) getArguments().getParcelable(EXTRA_CONTACT_URI) : null);
        } else {
            // If being recreated from a saved state, sets the contact from the incoming
            // savedInstanceState Bundle
            setContact((Uri) savedInstanceState.getParcelable(EXTRA_CONTACT_URI));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflates the main layout to be used by this fragment
        final View detailView =
                inflater.inflate(R.layout.fragment_contact_detail, container, false);
        // Gets handles to view objects in the layout
        mDetailLayout = (LinearLayout) detailView.findViewById(R.id.contact_detail_layout);
        return detailView;
    }

    public void setContact(Uri contactUri) {
        if (null != contactUri) {
            mContactUri = contactUri;
            getActivity().getSupportLoaderManager().restartLoader(ContactPhoneQuery.QUERY_ID, null, this);
            getActivity().getSupportLoaderManager().restartLoader(ContactAddressQuery.QUERY_ID, null, this);
        }
    }

    private LinearLayout buildEmptyItemLayout(int stringId) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                R.layout.list_item_text_only, mDetailLayout, false);
        TextView textView = (TextView) linearLayout.findViewById(R.id.id_item_name_tv);
        textView.setText(stringId);
        return linearLayout;
    }


    class PhoneItemViewHolder {
        ImageView avatar;
        ImageView icon;
        TextView name;
        TextView secondName;

        public PhoneItemViewHolder(View view) {
            secondName =
                    (TextView) view.findViewById(R.id.id_item_second_name_tv);
            name =
                    (TextView) view.findViewById(R.id.id_item_name_tv);
            avatar =
                    (ImageView) view.findViewById(R.id.id_item_av_iv);
            icon = (ImageView) view.findViewById(R.id.id_item_icon_iv);
        }
    }

    private LinearLayout buildPhoneItemLayout(int phoneType, String phoneTypeLabel, final String phoneNumber) {
        LinearLayout linearLayout;
        if (phoneTypeLabel == null && phoneType == 0) {
            linearLayout = buildEmptyItemLayout(R.string.not_find_phone_number);
        } else {
            linearLayout = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                    R.layout.list_item_avatar_with_text_and_icon_two_line, mDetailLayout, false);
            PhoneItemViewHolder phoneItemViewHolder = new PhoneItemViewHolder(linearLayout);
            phoneItemViewHolder.name.setText(phoneNumber);
            CharSequence label =
                    ContactsContract.CommonDataKinds.Phone.getTypeLabel(getResources(), phoneType, phoneTypeLabel);
            phoneItemViewHolder.secondName.setText(label);
            phoneItemViewHolder.avatar.setImageResource(R.drawable.ic_call_white);
            Utils.setImageViewFilter(phoneItemViewHolder.avatar, R.color.teal_500);
            phoneItemViewHolder.avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    // voice call
                                    if (Engine.getInstance().getSipService().isRegistered()) {
                                        ScreenAV.makeCall(phoneNumber,
                                                NgnMediaType.Audio,
                                                ZphoneApplication.getContext());
                                    }
                                }

                            }

                    ).start();
                }
            });
            phoneItemViewHolder.icon.setImageResource(R.drawable.ic_message_white);
            Utils.setImageViewFilter(phoneItemViewHolder.icon, R.color.cyan_500);
            phoneItemViewHolder.icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = Uri.parse("smsto:" + phoneNumber);
                    Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(sendIntent);
                }
            });
        }
        return linearLayout;
    }

    private LinearLayout buildAddressItemLayout(int addressType, String addressTypeLabel,
                                                final String address) {
        // Inflates the address layout
        final LinearLayout addressLayout =
                (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.list_item_avatar_with_text_two_line, mDetailLayout, false);

        // Gets handles to the view objects in the layout
        final TextView headerTextView =
                (TextView) addressLayout.findViewById(R.id.id_item_second_name_tv);
        final TextView addressTextView =
                (TextView) addressLayout.findViewById(R.id.id_item_name_tv);
        final ImageView addressImageView =
                (ImageView) addressLayout.findViewById(R.id.id_item_av_iv);
        addressImageView.setImageResource(R.drawable.ic_location_on_grey600);

        // If there's no addresses for the contact, shows the empty view and message, and hides the
        // header and button.
        if (addressTypeLabel == null && addressType == 0) {
            headerTextView.setVisibility(View.GONE);
            addressImageView.setVisibility(View.GONE);
            addressTextView.setText(R.string.no_address);
        } else {
            // Gets postal address label type
            CharSequence label =
                    StructuredPostal.getTypeLabel(getResources(), addressType, addressTypeLabel);

            // Sets TextView objects in the layout
            headerTextView.setText(label);
            addressTextView.setText(address);

            // Defines an onClickListener object for the address button
            addressImageView.setOnClickListener(new View.OnClickListener() {
                // Defines what to do when users click the address button
                @Override
                public void onClick(View view) {

                    final Intent viewIntent =
                            new Intent(Intent.ACTION_VIEW, constructGeoUri(address));

                    // A PackageManager instance is needed to verify that there's a default app
                    // that handles ACTION_VIEW and a geo Uri.
                    final PackageManager packageManager = getActivity().getPackageManager();

                    // Checks for an activity that can handle this intent. Preferred in this
                    // case over Intent.createChooser() as it will still let the user choose
                    // a default (or use a previously set default) for geo Uris.
                    if (packageManager.resolveActivity(
                            viewIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(viewIntent);
                    } else {
                        // If no default is found, displays a message that no activity can handle
                        // the view button.
                        Toast.makeText(getActivity(),
                                R.string.no_intent_found, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
        return addressLayout;
    }

    /**
     * Constructs a geo scheme Uri from a postal address.
     *
     * @param postalAddress A postal address.
     * @return the geo:// Uri for the postal address.
     */
    private Uri constructGeoUri(String postalAddress) {
        // Concatenates the geo:// prefix to the postal address. The postal address must be
        // converted to Uri format and encoded for special characters.
        return Uri.parse(GEO_URI_SCHEME_PREFIX + Uri.encode(postalAddress));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = Uri.withAppendedPath(mContactUri, Contacts.Data.CONTENT_DIRECTORY);
        switch (id) {
            case ContactAddressQuery.QUERY_ID:
                // This query loads contact address details, see
                // ContactAddressQuery for more information.
                return new CursorLoader(getActivity(), uri,
                        ContactAddressQuery.PROJECTION,
                        ContactAddressQuery.SELECTION,
                        null, null);
            case ContactPhoneQuery.QUERY_ID:
                return new CursorLoader(getActivity(), uri,
                        ContactPhoneQuery.PROJECTION,
                        ContactPhoneQuery.SELECTION,
                        null, null);

        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mContactUri == null) {
            return;
        }
        final LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        switch (loader.getId()) {
            case ContactAddressQuery.QUERY_ID:
                // Loops through all the rows in the Cursor
                if (data.moveToFirst()) {
                    do {
                        final LinearLayout layout = buildAddressItemLayout(
                                data.getInt(ContactAddressQuery.TYPE),
                                data.getString(ContactAddressQuery.LABEL),
                                data.getString(ContactAddressQuery.ADDRESS));
                        // Adds the new address layout to the details layout
                        mDetailLayout.addView(layout, layoutParams);
                    } while (data.moveToNext());
                } else {
                    // If nothing found, adds an empty address layout
                    Log.e("nothing", " adds an empty address layout");
                    //mDetailLayout.addView(buildEmptyItemLayout(R.string.not_find_address), layoutParams);
                }
                break;
            case ContactPhoneQuery.QUERY_ID:
                // Loops through all the rows in the Cursor
                mNumbers.clear();
                if (data.moveToFirst()) {
                    do {
                        final LinearLayout layout = buildPhoneItemLayout(
                                data.getInt(ContactPhoneQuery.TYPE),
                                data.getString(ContactPhoneQuery.LABEL),
                                data.getString(ContactPhoneQuery.NUMBER));
                        // Adds the new address layout to the details layout
                        mDetailLayout.addView(layout, layoutParams);
                        mNumbers.add(data.getString(ContactPhoneQuery.NUMBER));
                    } while (data.moveToNext());
                    ContactVoiceFragment voiceFragment = (ContactVoiceFragment) getActivity().getSupportFragmentManager()
                            .findFragmentByTag(
                                    "android:switcher:" + R.id.pager + ":2");
                    voiceFragment.setContact(mNumbers);
                } else {
                    // If nothing found, adds an empty address layout
                    //mDetailLayout.addView(buildEmptyItemLayout(R.string.not_find_phone_number), layoutParams);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onResume() {
        mDetailLayout.removeAllViews();
        getActivity().getSupportLoaderManager().restartLoader(ContactAddressQuery.QUERY_ID, null, this);
        getActivity().getSupportLoaderManager().restartLoader(ContactPhoneQuery.QUERY_ID, null, this);
        super.onResume();
    }


    public interface ContactPhoneQuery {
        final static int QUERY_ID = 3;
        final static String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
        };
        final static String SELECTION = Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
        final static int ID = 0;
        final static int NUMBER = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
    }

    /**
     * This interface defines constants used by address retrieval queries.
     */
    public interface ContactAddressQuery {
        // A unique query ID to distinguish queries being run by the
        // LoaderManager.
        final static int QUERY_ID = 2;

        // The query projection (columns to fetch from the provider)
        final static String[] PROJECTION = {
                StructuredPostal._ID,
                StructuredPostal.FORMATTED_ADDRESS,
                StructuredPostal.TYPE,
                StructuredPostal.LABEL,
        };

        // The query selection criteria. In this case matching against the
        // StructuredPostal content mime type.
        final static String SELECTION =
                Data.MIMETYPE + "='" + StructuredPostal.CONTENT_ITEM_TYPE + "'";

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int ADDRESS = 1;
        final static int TYPE = 2;
        final static int LABEL = 3;
    }

    public List<String> getmNumbers() {
        return mNumbers;
    }
}
