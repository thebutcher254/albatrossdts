package com.example.albatrossdts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CheckOut1.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CheckOut1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CheckOut1 extends Fragment {
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    //private static final String ARG_PARAM1 = "param1";
    //private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    //private String mParam1;
    //private String mParam2;

    //Views
    Button btnScanBarcode;
    TextView txtBarcodeNumber;
    TextView txtDocumentTitle;
    TextView txtDocumentDescription;
    RelativeLayout layoutScanResults;
    Button btnNext;


    //Shared prefs
    SharedPreferences sharedPreferences;
    SharedPreferences scannerSharedPreferences;

    //Firestore
    private FirebaseFirestore db;

    private OnFragmentInteractionListener mListener;

    public CheckOut1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CheckOut1.
     */
    // TODO: Rename and change types and number of parameters
    public static CheckOut1 newInstance(String param1, String param2) {
        CheckOut1 fragment = new CheckOut1();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
        sharedPreferences = getContext().getSharedPreferences("CheckOutDocumentData",0);

        //Initiate firestore instance
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_check_out1, container, false);

        //Instantiate views
        btnScanBarcode = view.findViewById(R.id.btnScanBarcode);
        txtBarcodeNumber = view.findViewById(R.id.txtBarcodeNumberCheckOut);
        txtDocumentTitle = view.findViewById(R.id.txtDocumentTitleCheckOut);
        txtDocumentDescription = view.findViewById(R.id.txtDocumentDescriptionCheckOut);
        layoutScanResults = view.findViewById(R.id.layoutScanResults);
        btnNext = view.findViewById(R.id.btnNextCheckOut1);

        //OnClick for scanbarcode button
        btnScanBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchBarcodeScanner();
            }
        });
        
        //OnClick for next button
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goNext();
            }
        });
        
        //If we have come from scanning, fill shared preferences with data about the scanned document from the database
        // and populate the views with the data.
        //We know we've come from scanning by checking the documentdata shared preference
        scannerSharedPreferences = getContext().getSharedPreferences("DocumentData",0);
        if(!scannerSharedPreferences.getString("barcode_number","").equals("")){
            //coming from scanning
            getDocumentData(scannerSharedPreferences.getString("barcode_number", ""));
        }

        return view;
    }

    private void goNext() {
        launchNextFragment();
    }

    private void launchNextFragment() {
        //Replaces the fragment in the frame_layout in app_bar_main.xml

        //See solution at https://stackoverflow.com/questions/13216916/how-to-replace-the-activitys-fragment-from-the-fragment-itself/13217087
        ((MainActivity)getActivity()).replaceFragment("CheckOut2",true);
    }

    private void getDocumentData(String barcode_number) {
        //
        //Query the database for the barcode that has been input.
        //If found, go to DeleteDocument2 fragment.
        //If not found, inform the user.

        db.collection("documents")
                .whereEqualTo("barcode_number",barcode_number)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            if (task.getResult().size()>0){
                                //The document was found
                                //Save the data in a Document object
                                Document document;
                                for(QueryDocumentSnapshot qds: task.getResult()){
                                    document = qds.toObject(Document.class);
                                    //Add the document's details to shared preferences
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("document_barcode_number",document.getBarcode_number());
                                    editor.putString("document_title",document.getTitle());
                                    editor.putString("document_description",document.getDescription());
                                    editor.putString("document_photo_url",document.getPhoto_url());
                                    editor.commit();

                                    //Clear the DocumentData shared preference
                                    editor = scannerSharedPreferences.edit();
                                    editor.clear();
                                    editor.commit();

                                    populateViews();

                                }


                            }else{
                                //Document not found
                                Toast.makeText(getContext(),"No item found with that barcode number.",Toast.LENGTH_LONG).show();

                            }

                        }else{
                            //TODO: Add error handling.

                        }
                    }
                });
    }

    private void populateViews() {
        txtBarcodeNumber.setText("Barcode number: "+sharedPreferences.getString("document_barcode_number",""));
        txtDocumentTitle.setText("Document title: "+sharedPreferences.getString("document_title",""));
        txtDocumentDescription.setText("Document description: "+sharedPreferences.getString("document_description",""));
        //make them visible
        layoutScanResults.setVisibility(View.VISIBLE);
        //enable the next button
        btnNext.setEnabled(true);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_REQUEST_CODE) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.addToBackStack(null);
                Intent intent = new Intent(getContext(),SimpleScannerActivity.class);
                intent.putExtra("fromFragment","CheckOut1");
                startActivity(intent);

            } else {

                Toast.makeText(getContext(), "Camera permission denied", Toast.LENGTH_LONG).show();

            }

        }
    }

    private void launchBarcodeScanner() {

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestPermissions(new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        else{
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.addToBackStack(null);
            Intent intent = new Intent(getContext(),SimpleScannerActivity.class);
            intent.putExtra("fromFragment","CheckOut1");
            startActivity(intent);
        }
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        clearScannerSharedPreferences();//So that when we come back it doesn't think it's come from scanning
    }


    private void clearScannerSharedPreferences(){
        //Clear the scanner shared preferences
        SharedPreferences.Editor editor = scannerSharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
