package com.zenkun.smsenhancer.ui;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map.Entry;

import com.zenkun.smsenhancer.preferences.ButtonListPreference;
import com.zenkun.smsenhancer.provider.SmsMmsMessage;
import com.zenkun.smsenhancer.util.Log;
import com.zenkun.smsenhancer.util.SmsPopupUtils;

import com.zenkun.smsenhancer.BuildConfig;
import com.zenkun.smsenhancer.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class SmsPopupFragment extends Fragment {
    private SmsMmsMessage message;
    private boolean messageViewed = false;

    private SmsPopupButtonsListener mButtonsListener;

    private TextView fromTv;
    private TextView timestampTv;
    private TextView messageTv;
    private LinearLayout mainLayout;
    private ScrollView contentMessage;
    private LinearLayout contentMms;
    private LinearLayout contentPrivacy;
    private int contentNum = 0;
    private float fontSize=14;

    private int privacyMode = PRIVACY_MODE_OFF;
    private boolean showUnlockButton = false;
    private boolean showButtons = true;

    private QuickContactBadge contactBadge;
    private ViewSwitcher buttonViewSwitcher;

    public static final int PRIVACY_MODE_OFF = 0;
    public static final int PRIVACY_MODE_HIDE_MESSAGE = 1;
    public static final int PRIVACY_MODE_HIDE_ALL = 2;

    private static final String EXTRA_PRIVACY_MODE = "com.zenkun.smsenhancer.privacy_mode";
    private static final String EXTRA_BUTTONS = "com.zenkun.smsenhancer.buttons";
    private static final String EXTRA_UNLOCK_BUTTON = "com.zenkun.smsenhancer.unlock_button";
    private static final String EXTRA_SHOW_BUTTONS = "com.zenkun.smsenhancer.show_buttons";

    private static final int VIEW_SMS = 0;
    private static final int VIEW_MMS = 1;
    private static final int VIEW_PRIVACY_SMS = 2;
    private static final int VIEW_PRIVACY_MMS = VIEW_MMS;

    public static final int BUTTON_VIEW = 100;
    public static final int BUTTON_VIEW_MMS = 101;
    public static final int BUTTON_UNLOCK = 102;
    public static final int BUTTON_PRIVACY = 103;

    private static final int CONTACT_IMAGE_FADE_DURATION = 300;

    private static final int BUTTON_SWITCHER_MAIN_BUTTONS = 0;
    private static final int BUTTON_SWITCHER_UNLOCK_BUTTON = 1;
    private static final String EMPTY_MMS_SUBJECT = "no subject";

    //sms enhancer
    private static SharedPreferences mPrefs=null;
    private int color=Color.WHITE;
    private boolean showIcsLine= true;
    private boolean enableBackground=false;
    private boolean enableCustomBackground=false;
    private String backgroundPath="";
    
    private static final HashMap<String, Integer> emoticons = new HashMap<String, Integer>();
    static {
  	  
  	  emoticons.put(";-)", R.drawable.emo_im_winking);
  	  emoticons.put(";)", R.drawable.emo_im_winking);
  	  emoticons.put(":-*)", R.drawable.emo_im_kissing);
  	  emoticons.put(":*)", R.drawable.emo_im_kissing);
  	  emoticons.put(":*", R.drawable.emo_im_kissing);
  	  emoticons.put(":)", R.drawable.emo_im_happy);
  	  emoticons.put(":-)", R.drawable.emo_im_happy);
  	  emoticons.put(":(", R.drawable.emo_im_sad);
  	  emoticons.put(":-(", R.drawable.emo_im_sad);
  	  emoticons.put(":'(", R.drawable.emo_im_crying);
  	  emoticons.put(":-$)", R.drawable.emo_im_money_mouth);
  	  emoticons.put(":$)", R.drawable.emo_im_money_mouth);
  	  emoticons.put("o:-)", R.drawable.emo_im_angel);
  	  emoticons.put(":-[", R.drawable.emo_im_embarrassed);
  	  emoticons.put(":d", R.drawable.emo_im_laughing);
  	  emoticons.put(":-d", R.drawable.emo_im_laughing);
  	  emoticons.put(":-x", R.drawable.emo_im_lips_are_sealed);
  	  emoticons.put(":x", R.drawable.emo_im_lips_are_sealed);
  	  emoticons.put(":eek:", R.drawable.emo_im_surprised);
  	  emoticons.put(":o", R.drawable.emo_im_surprised);
  	  emoticons.put(":-o", R.drawable.emo_im_surprised);
  	  emoticons.put("=-o", R.drawable.emo_im_surprised);
  	  emoticons.put(":p", R.drawable.emo_im_tongue_sticking_out);
  	  emoticons.put(":-p", R.drawable.emo_im_tongue_sticking_out);
  	  emoticons.put(":-\\", R.drawable.emo_im_undecided);
  	  emoticons.put(":-/", R.drawable.emo_im_undecided);
  	  emoticons.put(":confused:", R.drawable.emo_im_wtf);
  	  emoticons.put("o_o", R.drawable.emo_im_wtf);
  	  emoticons.put(":mad:", R.drawable.emo_im_yelling);
  	  emoticons.put("b-)", R.drawable.emo_im_cool);
  	  emoticons.put(":-!", R.drawable.emo_im_foot_in_mouth);
  	}
    public static Spannable getSmiledText(Context context, String text) {
  	  SpannableStringBuilder builder = new SpannableStringBuilder(text);
  	  int index;for (index = 0; index < builder.length(); index++) {
  	    for (Entry<String, Integer> entry : emoticons.entrySet()) {
  	      int length = entry.getKey().length();
  	      if (index + length > builder.length())
  	        continue;
  	      if (builder.subSequence(index, index + length).toString().toLowerCase().equals(entry.getKey())) {
  	        builder.setSpan(new ImageSpan(context, entry.getValue()), index, index + length,
  	            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
  	        index += length - 1;
  	        break;
  	      }
  	    }
  	  }
  	  return builder;
  	}
    private void setupPrefs()
    {
  	  	 mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
	  	 color= mPrefs.getInt(getActivity().getApplicationContext().getString(R.string.pref_colorPickerSMS_key), Color.WHITE);
	  	 showIcsLine = mPrefs.getBoolean(getActivity().getApplicationContext().getString(R.string.pref_showics_line_key), true);
	  	enableBackground = mPrefs.getBoolean(getString
        		(R.string.pref_enableBackgroundImage_key),false);
        enableCustomBackground = mPrefs.getBoolean(getString
        		(R.string.pref_enableBackgroundcustom_key),false);
        backgroundPath = mPrefs.getString(getString(R.string.pref_backgroundPath_key), "");
	  	 
	  	 String size="14";
	  	 try
	  	 {
	  		 size=mPrefs.getString(getActivity().getApplicationContext().getString(R.string.pref_font_size_key), "14");
	  		 fontSize = Float.parseFloat(size);
	  	 }catch (Exception e) {
	  		 Log.e("Error changing the Font Size");
	  		 e.printStackTrace();
	  		 fontSize = 14f;
	  	}
    }
    public static SmsPopupFragment newInstance(SmsMmsMessage newMessage, int[] buttons,
    		int privacyMode, boolean showUnlockButton, boolean showButtons) {
    	SmsPopupFragment newFragment = new SmsPopupFragment();
    	Bundle args = newMessage.toBundle();
    	args.putInt(EXTRA_PRIVACY_MODE, privacyMode);
    	args.putIntArray(EXTRA_BUTTONS, buttons);
    	args.putBoolean(EXTRA_UNLOCK_BUTTON, showUnlockButton);
    	args.putBoolean(EXTRA_SHOW_BUTTONS, showButtons);
    	newFragment.setArguments(args);
    	return newFragment;
    }

    public static SmsPopupFragment newInstance(SmsMmsMessage newMessage, int[] buttons) {
    	return newInstance(newMessage, buttons, PRIVACY_MODE_OFF, false, true);
    }

    public SmsPopupFragment() {};

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
		setupPrefs();
	    final Bundle args = getArguments();
        privacyMode = args.getInt(EXTRA_PRIVACY_MODE);
        showUnlockButton = args.getBoolean(EXTRA_UNLOCK_BUTTON);
        showButtons = args.getBoolean(EXTRA_SHOW_BUTTONS);
        final int[] buttons = args.getIntArray(EXTRA_BUTTONS);
        message = new SmsMmsMessage(getActivity(), args);

    	View v= inflater.inflate(R.layout.popup_message_fragment, container, false);
        // Find the main textviews and layouts
        fromTv = (TextView) v.findViewById(R.id.fromTextView);
        messageTv = (TextView) v.findViewById(R.id.messageTextView);
        timestampTv = (TextView) v.findViewById(R.id.timestampTextView);
        contentMessage = (ScrollView) v.findViewById(R.id.contentMessage);
        contentMms = (LinearLayout) v.findViewById(R.id.contentMms);
        contentPrivacy = (LinearLayout) v.findViewById(R.id.contentPrivacy);

        buttonViewSwitcher = (ViewSwitcher) v.findViewById(R.id.buttonViewSwitcher);
        mainLayout = (LinearLayout) v.findViewById(R.id.popupMessageMainlayout);
        // Find the QuickContactBadge view that will show the contact photo
        contactBadge = (QuickContactBadge) v.findViewById(R.id.contactBadge);
        //sms enhancer change color
        fromTv.setTextColor(color);
        messageTv.setTextColor(color);
        timestampTv.setTextColor(color);
        messageTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        if(!showIcsLine)
        {
        	View t = (View)v.findViewById(R.id.titleDivider);
        	t.setVisibility(View.INVISIBLE);
        }
//sms enhancer background Image
        
        boolean CustomImage=false;
        if (enableBackground) {
        	if (enableCustomBackground) {
        		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)&& backgroundPath!="") 
    			{
        		  File file = new File(backgroundPath);
      			  if(file.exists())
      			  {
      				  //si existe cargamos la imagen
      				  try
      				  {
      					 //Drawable bg =  new BitmapDrawable(decodeUri(Uri.parse("file://"+backgroundPath)));
      					Drawable bg =  new BitmapDrawable(SmsPopupUtils.resizeImg(Uri.parse("file://"+backgroundPath),getActivity().getContentResolver()));
      					
      					mainLayout.setBackgroundDrawable(bg);
      					 CustomImage=true;
      				  }catch (Exception e) {
      					  CustomImage=false;
      					  e.printStackTrace();
    					// TODO: handle exception
    				}
      				  
      			  }else
      			  {
      				  CustomImage=false;
      			  }
    			
    			}
    		} else {
    			CustomImage=false;
    		}
        	if(!CustomImage)
        	{
        		mainLayout.setBackgroundDrawable(getResources().getDrawable( R.drawable.jellybean_wallpaper));
        	}
        	
    		
    	}
        

        Log.v("Changing color and Size:"+String.valueOf(color)+ String.valueOf(fontSize) );
        final String[] buttonText = getResources().getStringArray(R.array.buttons_text);

        if (showButtons) {
            final Button button1 = (Button) v.findViewById(R.id.button1);
            final PopupButton button1Vals = new PopupButton(buttons[0], buttonText);
            button1.setOnClickListener(button1Vals);
            button1.setVisibility(button1Vals.buttonVisibility);
            button1.setText(button1Vals.buttonText);
            button1.setTextColor(color);

            final Button button2 = (Button) v.findViewById(R.id.button2);
            final PopupButton button2Vals = new PopupButton(buttons[1], buttonText);
            button2.setOnClickListener(button2Vals);
            button2.setVisibility(button2Vals.buttonVisibility);
            button2.setText(button2Vals.buttonText);
            button2.setTextColor(color);

            final Button button3 = (Button) v.findViewById(R.id.button3);
            final PopupButton button3Vals = new PopupButton(buttons[2], buttonText);
            button3.setOnClickListener(button3Vals);
            button3.setVisibility(button3Vals.buttonVisibility);
            button3.setText(button3Vals.buttonText);
            button3.setTextColor(color);
            /*
             * This is really hacky. There are two types of reply buttons (quick reply and reply).
             * If the user has selected to show both the replies then the text on the buttons
             * should be different. If they only use one then the text can just be "Reply".
             */
            int numReplyButtons = 0;
            if (button1Vals.isReplyButton)
                numReplyButtons++;
            if (button2Vals.isReplyButton)
                numReplyButtons++;
            if (button3Vals.isReplyButton)
                numReplyButtons++;

            if (numReplyButtons == 1) {
                if (button1Vals.isReplyButton)
                    button1.setText(R.string.button_reply);
                if (button2Vals.isReplyButton)
                    button2.setText(R.string.button_reply);
                if (button3Vals.isReplyButton)
                    button3.setText(R.string.button_reply);
            }

            ((Button) v.findViewById(R.id.unlockButton)).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mButtonsListener.onButtonClicked(BUTTON_UNLOCK);
                }
            });
        }

        if (message.isMms()) {
            // The ViewMMS button
            ((Button) v.findViewById(R.id.viewMmsButton)).setOnClickListener(
                    new PopupButton(BUTTON_VIEW_MMS, buttonText));
            final String mmsSubject = message.getMessageBody();
            final TextView mmsSubjectTV = ((TextView) v.findViewById(R.id.mmsSubjectTextView));
            if (mmsSubject != null && !"".equals(mmsSubject) && !EMPTY_MMS_SUBJECT.equals(mmsSubject)) {
                mmsSubjectTV.setText(message.getMessageBody());
            } else {
                mmsSubjectTV.setVisibility(View.GONE);
            }
        }

        // The privacy view message button
        ((ImageButton) v.findViewById(R.id.viewButton)).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mButtonsListener.onButtonClicked(BUTTON_VIEW);
                    }
                });

        populateViews();

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
        	mButtonsListener = (SmsPopupButtonsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
            		+ " must implement SmsPopupButtonsListener");
        }
    }

    /*
     * Populate all the main SMS/MMS views with content from the actual SmsMmsMessage
     */
    private void populateViews() {
        if (message.isSms()) {
            messageTv.setText(getSmiledText(getActivity().getApplicationContext()
            					, message.getMessageBody()));
        }

        // Set the from, message and header views
        fromTv.setText(message.getContactName());
        timestampTv.setText(message.getFormattedTimestamp());

        setPrivacy(privacyMode, true);
        refreshButtonViews();
    }

    public void resizeLayout(int newWidth, int screenWidth) {
        LayoutParams params = (LayoutParams) mainLayout.getLayoutParams();
        params.width = newWidth;
        mainLayout.setLayoutParams(params);
    }

    public void setShowUnlockButton(boolean show) {
        if (show != showUnlockButton) {
            showUnlockButton = show;
            refreshButtonViews();
        }
    }

    private void refreshButtonViews() {
        if (!showButtons) {
            buttonViewSwitcher.setVisibility(View.GONE);
        } else {
            final int currentView = buttonViewSwitcher.getDisplayedChild();
            if (showUnlockButton) {
                if (currentView != BUTTON_SWITCHER_UNLOCK_BUTTON) {
                    // Show unlock button
                    buttonViewSwitcher.setDisplayedChild(BUTTON_SWITCHER_UNLOCK_BUTTON);
                }
            } else {
                if (currentView != BUTTON_SWITCHER_MAIN_BUTTONS) {
                    // Show main popup buttons
                    buttonViewSwitcher.setDisplayedChild(BUTTON_SWITCHER_MAIN_BUTTONS);
                }
            }
        }
    }

    public void setPrivacy(int newMode) {
    	setPrivacy(newMode, false);
    }

    private void setPrivacy(int newMode, boolean initial) {

        if ((newMode != privacyMode || initial) && message != null) {
	        final int viewPrivacy =
	                message.isSms() ? VIEW_PRIVACY_SMS : VIEW_PRIVACY_MMS;

	        final int viewPrivacyOff =
	                message.isSms() ? VIEW_SMS : VIEW_MMS;

	        if (newMode == PRIVACY_MODE_OFF) {
                updateContentView(viewPrivacyOff);
	            fromTv.setVisibility(View.VISIBLE);
	            messageViewed = true;
	            if (initial || privacyMode == PRIVACY_MODE_HIDE_ALL) {
	            	loadContactPhoto();
	            }
	        } else if (newMode == PRIVACY_MODE_HIDE_MESSAGE) {
                updateContentView(viewPrivacy);
	            fromTv.setVisibility(View.VISIBLE);
	            loadContactPhoto();
	        } else if (newMode == PRIVACY_MODE_HIDE_ALL) {
	            updateContentView(viewPrivacy);
	            fromTv.setVisibility(View.GONE);
	        }
    	}
    	privacyMode = newMode;
    }

    private void updateContentView(int mode) {
        if (contentMessage != null && contentMms != null && contentPrivacy != null) {
            if (contentNum != mode) {
                contentNum = mode;
                switch (mode) {
                case VIEW_SMS:
                    contentMessage.setVisibility(View.VISIBLE);
                    contentMms.setVisibility(View.GONE);
                    contentPrivacy.setVisibility(View.GONE);
                    break;
                case VIEW_MMS:
                    contentMessage.setVisibility(View.GONE);
                    contentMms.setVisibility(View.VISIBLE);
                    contentPrivacy.setVisibility(View.GONE);
                    break;
                case VIEW_PRIVACY_SMS:
                    contentMessage.setVisibility(View.GONE);
                    contentMms.setVisibility(View.GONE);
                    contentPrivacy.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }
    }

    private void loadContactPhoto() {
    	boolean cacheHit = false;
    	if (mButtonsListener != null && message.getContactLookupUri() != null) {
    		final LruCache<Uri, Bitmap> cache = mButtonsListener.getCache();
    		if (cache != null) {
    			final Bitmap bitmap = cache.get(message.getContactLookupUri());
    			if (bitmap != null) {
    				if (BuildConfig.DEBUG) Log.v("loadContactPhoto() - bitmap cache hit");
    				contactBadge.setImageBitmap(bitmap);
    				cacheHit = true;
    			}
    		}
    	}

    	if (!cacheHit) {
    		new FetchContactPhotoTask(contactBadge).execute(message.getContactLookupUri());
    	}

        contactBadge.setClickable(true);
        final Uri contactUri = message.getContactLookupUri();
        if (contactUri != null) {
            contactBadge.assignContactUri(message.getContactLookupUri());
        } else {
            contactBadge.assignContactFromPhone(message.getAddress(), false);
        }
    }

    /**
     * AsyncTask to fetch contact photo in background
     */
    private class FetchContactPhotoTask extends AsyncTask<Uri, Integer, Bitmap> {
    	private final WeakReference<QuickContactBadge> viewReference;

    	public FetchContactPhotoTask(QuickContactBadge badge) {
    		viewReference = new WeakReference<QuickContactBadge> (badge);
    	}

        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (isAdded()) {
                if (BuildConfig.DEBUG)
                    Log.v("Loading contact photo in background...");
                final Bitmap bitmap = SmsPopupUtils.getPersonPhoto(getActivity(), params[0]);
            	if (mButtonsListener != null && bitmap != null) {
            		final LruCache<Uri, Bitmap> cache = mButtonsListener.getCache();
            		if (cache != null) {
            			cache.put(params[0], bitmap);
            		}
            	}

                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap photo) {
            if (BuildConfig.DEBUG)
                Log.v("Done loading contact photo");
            if (photo != null && viewReference != null) {
            	final QuickContactBadge badge = viewReference.get();
            	if (badge != null && isAdded()) {
	                TransitionDrawable mTd =
	                        new TransitionDrawable(new Drawable[] {
	                                getResources().getDrawable(R.drawable.ic_contact_picture),
	                                new BitmapDrawable(getResources(), photo) });
	                badge.setImageDrawable(mTd);
	                mTd.setCrossFadeEnabled(false);
	                mTd.startTransition(CONTACT_IMAGE_FADE_DURATION);
            	}
            }
        }
    }

    private class PopupButton implements OnClickListener {
        final private int buttonId;
        public boolean isReplyButton;
        public String buttonText;
        public int buttonVisibility = View.VISIBLE;

        public PopupButton(int id, String[] buttonTextArray) {
            buttonId = id;
            isReplyButton = false;
            if (buttonId == ButtonListPreference.BUTTON_REPLY
                    || buttonId == ButtonListPreference.BUTTON_QUICKREPLY
                    || buttonId == ButtonListPreference.BUTTON_REPLY_BY_ADDRESS) {
                isReplyButton = true;
            }

            if (buttonId < buttonTextArray.length) {
            	buttonText = buttonTextArray[buttonId];
            }

            if (buttonId == ButtonListPreference.BUTTON_DISABLED) { // Disabled
                buttonVisibility = View.GONE;
            }
        }

        @Override
        public void onClick(View v) {
        	mButtonsListener.onButtonClicked(buttonId);
        }
    }

    public static interface SmsPopupButtonsListener {
    	abstract void onButtonClicked(int buttonType);
    	abstract LruCache<Uri, Bitmap> getCache();
    }

}
