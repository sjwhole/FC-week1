package com.flowcamp.tab.ui.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.flowcamp.tab.Phone;
import com.flowcamp.tab.PhoneListViewAdapter;
import com.flowcamp.tab.R;
import com.flowcamp.tab.databinding.FragmentPhoneBinding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class PhoneFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private FragmentPhoneBinding binding;
    private Context context;
    private ArrayList<Phone> phoneArrayList;

    public PhoneFragment(Context context) {
        this.context = context;
    }

    public static PhoneFragment newInstance(Context context, int index) {
        PhoneFragment fragment = new PhoneFragment(context);
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @SuppressLint("WrongThread")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View rootView = inflater.inflate(R.layout.fragment_phone, container, false);

        phoneArrayList = new ArrayList<>();

        Uri uri = CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.Contacts._ID,
                CommonDataKinds.Phone.DISPLAY_NAME,
                CommonDataKinds.Phone.NUMBER,
        };

        String sortOrder = CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
        String[] PHOTO_BITMAP_PROJECTION = new String[] {
                CommonDataKinds.Photo.PHOTO
        };

        try {
            @SuppressLint("Recycle") Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortOrder);
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    Bitmap photo = null;

                    photo = getFacebookPhoto(context, cursor.getString(2));

                    Phone phone = new Phone(
                            (int) cursor.getLong(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            photo);
                    phoneArrayList.add(phone);

                } while (cursor.moveToNext());
            }
            cursor.close();
        } catch (SecurityException ignored) {
            ignored.printStackTrace();
            phoneArrayList.add(new Phone(1, "연락처 접근 권한을 허용해주세요", "", null));
        }

        PhoneListViewAdapter adapter = new PhoneListViewAdapter(context, phoneArrayList);


        ListView listView = (ListView) rootView.findViewById(R.id.list);
        listView.setAdapter(adapter);

        rootView.findViewById(R.id.add_contact).setOnClickListener(
                view -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("전화번호 추가하기");
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    final EditText nameBox = new EditText(context);
                    nameBox.setHint("이름");
                    layout.addView(nameBox);

                    final EditText numberBox = new EditText(context);
                    numberBox.setHint("전화번호");
                    layout.addView(numberBox);

                    builder.setView(layout);

                    builder.setPositiveButton("추가", (dialog, which) -> addContact(nameBox.getText().toString(), numberBox.getText().toString()));
                    builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

                    builder.show();

                }
        );

        SearchView searchView = (SearchView) rootView.findViewById(R.id.searchContact);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                for (int i = 0; i < phoneArrayList.size(); i++) {
                    if (phoneArrayList.get(i).getName().contains(query)) {
                        listView.setSelection(i);
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                for (int i = 0; i < phoneArrayList.size(); i++) {
                    if (phoneArrayList.get(i).getName().contains(newText)) {
                        listView.setSelection(i);
                        break;
                    }
                }
                return true;
            }
        });

        rootView.findViewById(R.id.add_contact).setOnClickListener(
                view -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("전화번호 추가하기");
                    LinearLayout layout = new LinearLayout(context);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    final EditText nameBox = new EditText(context);
                    nameBox.setHint("이름");
                    layout.addView(nameBox);

                    final EditText numberBox = new EditText(context);
                    numberBox.setHint("전화번호");
                    layout.addView(numberBox);

                    builder.setView(layout);

                    builder.setPositiveButton("추가", (dialog, which) -> addContact(nameBox.getText().toString(), numberBox.getText().toString()));
                    builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

                    builder.show();

                }
        );

        return rootView;
    }

    public Bitmap getFacebookPhoto(Context context, String phoneNumber) {
        Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Uri photoUri = null;
        ContentResolver cr = context.getContentResolver();
        Cursor contact = cr.query(phoneUri,
                new String[] { ContactsContract.Contacts._ID }, null, null, null);

        Bitmap defaultPhoto = null;
        if (contact.moveToFirst()) {
            @SuppressLint("Range") long userId = contact.getLong(contact.getColumnIndex(ContactsContract.Contacts._ID));
            photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, userId);

        }
        else {
//            Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_report_image);
            return defaultPhoto;
        }
        if (photoUri != null) {
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
                    cr, photoUri);
            if (input != null) {
                return BitmapFactory.decodeStream(input);
            }
        } else {
//            Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_report_image);
            return defaultPhoto;
        }
//        Bitmap defaultPhoto = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_report_image);
        return defaultPhoto;
    }

    public void addContact(String name, String number) {
        Intent intent = new Intent(
                ContactsContract.Intents.SHOW_OR_CREATE_CONTACT,
                ContactsContract.Contacts.CONTENT_URI);
        intent.setData(Uri.parse("tel:" + number));//specify your number here
        intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
        startActivity(intent);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}