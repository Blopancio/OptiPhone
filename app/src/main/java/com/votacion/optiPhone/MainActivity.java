package com.votacion.optiPhone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.RETR_TREE;


public class MainActivity extends AppCompatActivity {

    Mat imageMatGray;
    Mat imageMat;
    Mat originalImageMat;
    ImageView result;
    EditText erosion;
    Bitmap imageBitmap;
    Point leftRectUp;
    Point leftRectDown;
    Point rightRect;
    int candidates;
    final Context context = this;
    List<Point> votes;
    HashMap<Integer, Integer> countingVotes;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.votacion.optiPhone.R.layout.activity_main);

        result = (ImageView)findViewById(com.votacion.optiPhone.R.id.imageView);
        erosion = (EditText)findViewById(com.votacion.optiPhone.R.id.erosion);
        countingVotes = new HashMap<>();


    }

    public void dispatchTakePictureIntent(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }



    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    imageMat = new Mat();
                    imageMatGray = new Mat();
                    originalImageMat = new Mat();
                    candidates = 15;
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            result.setImageBitmap(process(3));
        }
    }

    private Bitmap process(int i) {
        Utils.bitmapToMat(imageBitmap, imageMat);
        Imgproc.cvtColor(imageMat, imageMatGray, Imgproc.COLOR_BGR2GRAY);
        votes = new ArrayList<>();
        canny(i);
        contour();
        Bitmap bitMap = imageBitmap.copy(imageBitmap.getConfig(),true);
        Utils.matToBitmap(imageMat, bitMap);
        return bitMap;
    }

    public void contour(){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Mat imageMatGrayCopy = imageMatGray.clone();
        Imgproc.findContours(imageMatGrayCopy, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            rectangles(contours.get(contourIdx));
        }
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            Scalar scalar = getScalarAndPoint(contours.get(contourIdx));
            Imgproc.drawContours(imageMat, contours, contourIdx, scalar);
        }
        Log.d("Image", leftRectUp.toString());
        Log.d("Image", leftRectDown.toString());
        Log.d("Image", rightRect.toString());

    }

    private Scalar getScalarAndPoint(MatOfPoint matOfPoint) {
        if (matOfPoint.toArray()[0].equals(leftRectUp))
            return new Scalar(255, 255, 0);
        if(matOfPoint.toArray()[0].equals(leftRectDown))
            return new Scalar(255, 0, 0);
        if (matOfPoint.toArray()[0].equals(rightRect))
            return new Scalar(0, 0, 255);
        votes.add(matOfPoint.toArray()[0]);
        Log.d("Images", matOfPoint.toArray()[0].toString());
        Log.d("Images", votes.size()+"");
        return new Scalar(0,255,0);
    }

    private Point getPoint(MatOfPoint matOfPoint){
        return matOfPoint.toArray()[0];
    }
    private void rectangles(MatOfPoint matOfPoint) {
        compareLeftUp(getPoint(matOfPoint));
        compareLeftDown(getPoint(matOfPoint));
        compareRight(getPoint(matOfPoint));
    }

    private void compareRight(Point point) {
        if(rightRect== null){
            rightRect= point;
            return;
        }
        double x = point.x;
        if(x>rightRect.x)
                rightRect=point;
    }

    private void compareLeftUp(Point point) {
        if(leftRectUp== null){
            leftRectUp= point;
            return;
        }
        double x = point.x;
        double y = point.y;
        if(Math.abs(x-leftRectUp.x)< 10){
            if(y<leftRectUp.y)
                leftRectUp=point;
        }
    }

    private void compareLeftDown(Point point) {
        if(leftRectDown== null){
            leftRectDown= point;
            return;
        }
        double x = point.x;
        double y = point.y;
        if(Math.abs(x-leftRectDown.x)< 10){
            if(y>leftRectDown.y)
                leftRectDown=point;
        }
    }

    public void canny(int erosion){
        for(int i=0; i<erosion; i++) {
            Imgproc.dilate(imageMatGray, imageMatGray, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        }
        Imgproc.GaussianBlur(imageMatGray, imageMatGray, new Size(3, 3), 0);
        Imgproc.adaptiveThreshold(imageMatGray, imageMatGray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
    }

    public void reprocess(View view) {
        if(imageBitmap!=null) {
            Log.d("Image", "reprocessing");
            rightRect= null;
            leftRectDown=null;
            leftRectUp=null;
            result.setImageBitmap(process(erosion.getText().length() == 0 ? 3 : Integer.parseInt(erosion.getText().toString())));
        }
    }

    public void getVote(View view) {
        if(votes == null || votes.isEmpty()) return;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Casting vote");
        final List<Integer> candidates = getCandidates();
        StringBuilder stringVote = new StringBuilder();
        for(int i=candidates.size()-1; i>= 0; i--) {
            if(i== 0) {
                stringVote.append(" and candidate ").append(candidates.get(i));
                continue;
            }
            if(i!= candidates.size()-1) stringVote.append(", ");
            stringVote.append("candidate ").append(candidates.get(i));
        }
        alertDialogBuilder
                .setMessage("Do you want to vote for "+ stringVote.toString()+"?")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        for (int c: candidates)
                            castVote(c);
                    }


                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void castVote(int c){
        if(countingVotes.containsKey(c)){
            countingVotes.put(c, countingVotes.get(c)+1);
            return;
        }
        countingVotes.put(c, 1);
    }

    public void getResults(View view) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Results");

        StringBuilder stringVote = new StringBuilder();
        for(int i=1; i<candidates+1; i++) {
            int res= countingVotes.containsKey(i)? countingVotes.get(i): 0;
            stringVote.append("Candidate ").append(i).append(": ").append(res).append("\n");
        }

        alertDialogBuilder
                .setMessage(stringVote.toString())
                .setCancelable(false)
                .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }


                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public List<Integer> getCandidates() {
        List<Integer> res = new ArrayList<>();
        double heightPhoto = Math.abs(leftRectDown.y - leftRectUp.y);
        double minLeft = Math.min(leftRectUp.x,leftRectDown.x);
        double minUp = leftRectUp.y;
        double widthPhoto = Math.abs(rightRect.x - minLeft);

        for(Point p: votes){

            int valX = (int) Math.floor((p.x)*3/widthPhoto);
            int valY = (int) Math.floor((p.y-minUp)*5/heightPhoto);
            int val = valY*3 + valX +1;
            res.add(val);
            Log.d("Images", "Point: "+ p.toString());
            Log.d("Images", "valX: "+ valX+" valY: "+ valY+ " val: "+ val);
        }
        return res;
    }

}
