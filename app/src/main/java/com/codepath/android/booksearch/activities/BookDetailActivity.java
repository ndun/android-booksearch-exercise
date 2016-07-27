package com.codepath.android.booksearch.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.codepath.android.booksearch.R;
import com.codepath.android.booksearch.models.Book;
import com.codepath.android.booksearch.net.BookClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;

public class BookDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BOOK = "BOOK";

    private ImageView ivBookCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private ShareActionProvider miShareAction;
    private Intent shareIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        // Fetch views
        ivBookCover = (ImageView) findViewById(R.id.ivBookCover);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvAuthor = (TextView) findViewById(R.id.tvAuthor);

        // Extract book object from intent extras
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Book aBook = (Book) extras.get(EXTRA_BOOK);
        tvTitle.setText(aBook.getTitle());
        tvAuthor.setText(aBook.getAuthor());

        Picasso.with(this).load(aBook.getCoverUrl()).into(ivBookCover, new Callback() {
            @Override
            public void onSuccess() {
                // Setup share intent now that image has loaded
                prepareShareIntent();
                attachShareIntentAction();
            }

            @Override
            public void onError() {
                // ...
            }
        });

        // Use book object to populate data into views
        getSupportActionBar().setTitle(aBook.getTitle());
        getExtraBookInfo(aBook);
    }

    private void getExtraBookInfo(final Book aBook) {
        final String bookId = aBook.getOpenLibraryId();
        BookClient client = new BookClient();
        client.getBookDetails(bookId, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if(response != null) {
                        Log.d("response: ", response.toString());
                        JSONObject bookDetails = response.getJSONObject("OLID:" + bookId);
                        Log.d("testing details", bookDetails.toString());
                        aBook.populateDetails(bookDetails);
                        TextView tvPublishingInfo = (TextView) findViewById(R.id.tvPublishingInfo);
                        tvPublishingInfo.setText("Published on " + aBook.getPublishedDate() + " by " + aBook.getPublisher());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    // Gets the image URI and setup the associated share intent to hook into the provider
    public void prepareShareIntent() {
        // Fetch Bitmap Uri locally
        ImageView ivImage = (ImageView) findViewById(R.id.ivBookCover);
        Uri bmpUri = getLocalBitmapUri(ivImage); // see previous remote images section
        TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
        String title = tvTitle.getText().toString();
        // Construct share intent as described above based on bitmap
        shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.putExtra(Intent.EXTRA_TITLE, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, title);
//        shareIntent.setType("image/*");
        shareIntent.setType("*/*");
    }

    // Attaches the share intent to the share menu item provider
    public void attachShareIntentAction() {
        if (miShareAction != null && shareIntent != null)
            miShareAction.setShareIntent(shareIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_book_detail, menu);
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch reference to the share action provider
        miShareAction = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        attachShareIntentAction();
        // Return true to display menu
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Returns the URI path to the Bitmap displayed in specified ImageView
    public Uri getLocalBitmapUri(ImageView imageView) {
        // Extract Bitmap from ImageView drawable
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp = null;
        if (drawable instanceof BitmapDrawable){
            bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        } else {
            return null;
        }
        // Store image to default external storage directory
        Uri bmpUri = null;
        try {
            // Use methods on Context to access package-specific directories on external storage.
            // This way, you don't need to request external read/write permission.
            // See https://youtu.be/5xVh-7ywKpE?t=25m25s
            File file =  new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            bmpUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bmpUri;
    }
}
