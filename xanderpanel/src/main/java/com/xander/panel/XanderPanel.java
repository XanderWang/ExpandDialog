/*
 * Copyright (C) 2013 Lemon Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xander.panel;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.xander.panel.PanelController.XanderParams;

public class XanderPanel extends Dialog implements DialogInterface.OnKeyListener {

    private static final String TAG = "XanderPanel";

    private static final int TRANSLATE_DIALOG = R.style.XanderPanelLight;

    public enum THEME {
        NORMAL, MENU, SHEET
    }

    private PanelController panelController;

    public static final int MSG_SHOW_DIALOG = 0;
    public static final int MSG_DISMISS_DIALOG = 1;
    public static final int MSG_DISMISS_CANCEL = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_DISMISS_CANCEL:
                case MSG_DISMISS_DIALOG:
                    realDismiss();
                    break;
                default:
                    break;
            }
        }
    };

    protected boolean mDismissing;
    private boolean mCancelable = true;

    /**
     * 背景透明度
     */
    private static final float DEFAULT_DIM_AMOUNT = 0.0f;

    SystemBarTintManager tintManager = null;

    private XanderPanel(Context context) {
        super(context,TRANSLATE_DIALOG);
        if( null == tintManager ) {
//            tintManager = new SystemBarTintManager(context,getWindow());
//            tintManager.setStatusBarTintEnabled(true);
//            tintManager.setStatusBarTintColor(0x0000ff00);
//            tintManager.setNavigationBarTintEnabled(true);
//            tintManager.setNavigationBarTintColor(0x00ff0000);
//            tintManager.setTintAlpha(0.f);
        }
        setDimAmount(DEFAULT_DIM_AMOUNT);
        panelController = new PanelController(getContext(), this);
    }

    private void setDimAmount(float dimAmount) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount = dimAmount;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        TypedArray a = getContext().obtainStyledAttributes(new int[]{android.R.attr.layout_width});
        try {
            params.width = a.getLayoutDimension(0, ViewGroup.LayoutParams.MATCH_PARENT);
        } finally {
            a.recycle();
        }
        getWindow().setAttributes(params);

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setNavigationBarColor(0x33000000);
            getWindow().setStatusBarColor(0x33000000);
        }
    }

    /**
     * Show the dialog, dimming the screen and expanding the button menu
     */
    @Override
    public void show() {
        super.show();
        mDismissing = false;
        panelController.animateShow();
    }

    private void realDismiss() {
        super.dismiss();
    }

    /**
     * Dismiss the dialog, removing screen dim and hiding the expanded menu
     */
    @Override
    public void dismiss() {
        mDismissing = true;
        panelController.animateDismiss();

//        tintManager.setStatusBarTintEnabled(false);
//        tintManager.setNavigationBarTintEnabled(false);

        mHandler.sendEmptyMessageDelayed(MSG_DISMISS_DIALOG, PanelController.DURATION);
    }

    @Override
    public void cancel() {
        panelController.animateDismiss();
        mHandler.sendEmptyMessageDelayed(MSG_DISMISS_DIALOG, PanelController.DURATION);
    }

    @Override
    public void setOnKeyListener(OnKeyListener onKeyListener) {
        super.setOnKeyListener(this);
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP
                && !event.isCanceled() && mCancelable && !mDismissing) {
            dismiss();
            return true;
        }
        return false;
    }

    /**
     * Gets the list view used in the dialog.
     *
     * @return The {@link ListView} from the dialog.
     */
    public ListView getListView() {
        return panelController.getListView();
    }


    public static class Builder {
        private final XanderParams mXanderParams;
        private int mTheme;

        /**
         * Constructor using a context for this builder and the {@link XanderPanel} it creates.
         */
        public Builder(Context context) {
            this(context, 0);
        }

        /**
         * Constructor using a context and theme for this builder and
         * the {@link XanderPanel} it creates.  The actual theme
         * that an XanderPanel uses is a private implementation, however you can
         * here supply either the name of an attribute in the theme from which
         * to get the dialog's style or one of the constants
         */
        public Builder(Context context, int theme) {
            mXanderParams = new XanderParams(context);
            mTheme = theme;
            setPanleMargen(context.getResources().getDimensionPixelSize(R.dimen.panel_margen));
        }

        /**
         * Returns a {@link Context} with the appropriate theme for dialogs created by this Builder.
         * Applications should use this Context for obtaining LayoutInflaters for inflating views
         * that will be used in the resulting dialogs, as it will cause views to be inflated with
         * the correct theme.
         *
         * @return A Context for built Dialogs.
         */
        public Context getContext() {
            return mXanderParams.mContext;
        }

        /**
         * Set the title using the given resource id.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTitle(int titleId) {
            mXanderParams.mTitle = mXanderParams.mContext.getText(titleId);
            return this;
        }

        /**
         * Set the title displayed in the {@link Dialog}.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setTitle(CharSequence title) {
            mXanderParams.mTitle = title;
            return this;
        }

        /**
         * Set the title using the custom view {@code customTitleView}. The
         * methods {@link #setTitle(int)} and {@link #setIcon(int)} should be
         * sufficient for most titles, but this is provided if the title needs
         * more customization. Using this will replace the title and icon set
         * via the other methods.
         *
         * @param customTitleView The custom view to use as the title.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCustomTitle(View customTitleView) {
            mXanderParams.mCustomTitleView = customTitleView;
            return this;
        }

        /**
         * Set the message to display using the given resource id.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMessage(int messageId) {
            mXanderParams.mMessage = mXanderParams.mContext.getText(messageId);
            return this;
        }

        /**
         * Set the message to display.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMessage(CharSequence message) {
            mXanderParams.mMessage = message;
            return this;
        }

        /**
         * Set the resource id of the {@link Drawable} to be used in the title.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setIcon(int iconId) {
            mXanderParams.mIconId = iconId;
            return this;
        }

        /**
         * Set the {@link Drawable} to be used in the title.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setIcon(Drawable icon) {
            mXanderParams.mIcon = icon;
            return this;
        }

        /**
         * Set an icon as supplied by a theme attribute. e.g. android.R.attr.ExpandDialogIcon
         *
         * @param attrId ID of a theme attribute that points to a drawable resource.
         */
        public Builder setIconAttribute(int attrId) {
            TypedValue out = new TypedValue();
            mXanderParams.mContext.getTheme().resolveAttribute(attrId, out, true);
            mXanderParams.mIconId = out.resourceId;
            return this;
        }

        /**
         * Sets whether the dialog is cancelable or not.  Default is true.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCancelable(boolean cancelable) {
            mXanderParams.mCancelable = cancelable;
            return this;
        }

        public Builder setGravity(int gravity) {
            mXanderParams.mGravity = gravity;
            return this;
        }

        public Builder setCanceledOnTouchOutside(boolean outside) {
            mXanderParams.mCanceledOnTouchOutside = outside;
            return this;
        }

        public Builder setPanleMargen(int margen) {
            mXanderParams.mPanelMargen = margen;
            return this;
        }

        /**
         * Sets the callback that will be called if the dialog is canceled.
         * <p/>
         * <p>Even in a cancelable dialog, the dialog may be dismissed for reasons other than
         * being canceled or one of the supplied choices being selected.
         * If you are interested in listening for all cases where the dialog is dismissed
         * and not just when it is canceled, see
         * {@link #setOnDismissListener(DialogInterface.OnDismissListener) setOnDismissListener}.</p>
         *
         * @return This Builder object to allow for chaining of calls to set methods
         * @see #setCancelable(boolean)
         * @see #setOnDismissListener(android.content.DialogInterface.OnDismissListener)
         */
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            mXanderParams.mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Sets the callback that will be called when the dialog is dismissed for any reason.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnDismissListener(OnDismissListener onDismissListener) {
            mXanderParams.mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Sets the callback that will be called if a key is dispatched to the dialog.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnKeyListener(OnKeyListener onKeyListener) {
            mXanderParams.mOnKeyListener = onKeyListener;
            return this;
        }

        /**
         * Sets the callback that will be called if the dialog is show.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnShowListener(OnShowListener onShowListener) {
            mXanderParams.mOnShowListener = onShowListener;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of the
         * selected item via the supplied listener. This should be an array type i.e. R.array.foo
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setItems(int itemsId, final OnClickListener listener) {
            mXanderParams.mItems = mXanderParams.mContext.getResources().getTextArray(itemsId);
            mXanderParams.mOnClickListener = listener;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of the
         * selected item via the supplied listener.
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setItems(CharSequence[] items, final OnClickListener listener) {
            mXanderParams.mItems = items;
            mXanderParams.mOnClickListener = listener;
            return this;
        }

        /**
         * Set a list of items, which are supplied by the given {@link ListAdapter}, to be
         * displayed in the dialog as the content, you will be notified of the
         * selected item via the supplied listener.
         *
         * @param adapter  The {@link ListAdapter} to supply the list of items
         * @param listener The listener that will be called when an item is clicked.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setAdapter(final ListAdapter adapter, final OnClickListener listener) {
            mXanderParams.mAdapter = adapter;
            mXanderParams.mOnClickListener = listener;
            return this;
        }

        /**
         * Set a list of items, which are supplied by the given {@link Cursor}, to be
         * displayed in the dialog as the content, you will be notified of the
         * selected item via the supplied listener.
         *
         * @param cursor      The {@link Cursor} to supply the list of items
         * @param listener    The listener that will be called when an item is clicked.
         * @param labelColumn The column name on the cursor containing the string to display
         *                    in the label.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setCursor(final Cursor cursor, final OnClickListener listener, String labelColumn) {
            mXanderParams.mCursor = cursor;
            mXanderParams.mLabelColumn = labelColumn;
            mXanderParams.mOnClickListener = listener;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content,
         * you will be notified of the selected item via the supplied listener.
         * This should be an array type, e.g. R.array.foo. The list will have
         * a check mark displayed to the right of the text for each checked
         * item. Clicking on an item in the list will not dismiss the dialog.
         * Clicking on a button will dismiss the dialog.
         *
         * @param itemsId      the resource id of an array i.e. R.array.foo
         * @param checkedItems specifies which items are checked. It should be null in which case no
         *                     items are checked. If non null it must be exactly the same length as the array of
         *                     items.
         * @param listener     notified when an item on the list is clicked. The dialog will not be
         *                     dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                     button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems, final OnMultiChoiceClickListener listener) {
            mXanderParams.mItems = mXanderParams.mContext.getResources().getTextArray(itemsId);
            mXanderParams.mOnCheckboxClickListener = listener;
            mXanderParams.mCheckedItems = checkedItems;
            mXanderParams.mIsMultiChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content,
         * you will be notified of the selected item via the supplied listener.
         * The list will have a check mark displayed to the right of the text
         * for each checked item. Clicking on an item in the list will not
         * dismiss the dialog. Clicking on a button will dismiss the dialog.
         *
         * @param items        the text of the items to be displayed in the list.
         * @param checkedItems specifies which items are checked. It should be null in which case no
         *                     items are checked. If non null it must be exactly the same length as the array of
         *                     items.
         * @param listener     notified when an item on the list is clicked. The dialog will not be
         *                     dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                     button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMultiChoiceItems(CharSequence[] items, boolean[] checkedItems, final OnMultiChoiceClickListener listener) {
            mXanderParams.mItems = items;
            mXanderParams.mOnCheckboxClickListener = listener;
            mXanderParams.mCheckedItems = checkedItems;
            mXanderParams.mIsMultiChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content,
         * you will be notified of the selected item via the supplied listener.
         * The list will have a check mark displayed to the right of the text
         * for each checked item. Clicking on an item in the list will not
         * dismiss the dialog. Clicking on a button will dismiss the dialog.
         *
         * @param cursor          the cursor used to provide the items.
         * @param isCheckedColumn specifies the column name on the cursor to use to determine
         *                        whether a checkbox is checked or not. It must return an integer value where 1
         *                        means checked and 0 means unchecked.
         * @param labelColumn     The column name on the cursor containing the string to display in the
         *                        label.
         * @param listener        notified when an item on the list is clicked. The dialog will not be
         *                        dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                        button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setMultiChoiceItems(Cursor cursor, String isCheckedColumn, String labelColumn, final OnMultiChoiceClickListener listener) {
            mXanderParams.mCursor = cursor;
            mXanderParams.mOnCheckboxClickListener = listener;
            mXanderParams.mIsCheckedColumn = isCheckedColumn;
            mXanderParams.mLabelColumn = labelColumn;
            mXanderParams.mIsMultiChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of
         * the selected item via the supplied listener. This should be an array type i.e.
         * R.array.foo The list will have a check mark displayed to the right of the text for the
         * checked item. Clicking on an item in the list will not dismiss the dialog. Clicking on a
         * button will dismiss the dialog.
         *
         * @param itemsId     the resource id of an array i.e. R.array.foo
         * @param checkedItem specifies which item is checked. If -1 no items are checked.
         * @param listener    notified when an item on the list is clicked. The dialog will not be
         *                    dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                    button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setSingleChoiceItems(int itemsId, int checkedItem, final OnClickListener listener) {
            mXanderParams.mItems = mXanderParams.mContext.getResources().getTextArray(itemsId);
            mXanderParams.mOnClickListener = listener;
            mXanderParams.mCheckedItem = checkedItem;
            mXanderParams.mIsSingleChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of
         * the selected item via the supplied listener. The list will have a check mark displayed to
         * the right of the text for the checked item. Clicking on an item in the list will not
         * dismiss the dialog. Clicking on a button will dismiss the dialog.
         *
         * @param cursor      the cursor to retrieve the items from.
         * @param checkedItem specifies which item is checked. If -1 no items are checked.
         * @param labelColumn The column name on the cursor containing the string to display in the
         *                    label.
         * @param listener    notified when an item on the list is clicked. The dialog will not be
         *                    dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                    button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setSingleChoiceItems(Cursor cursor, int checkedItem, String labelColumn, final OnClickListener listener) {
            mXanderParams.mCursor = cursor;
            mXanderParams.mOnClickListener = listener;
            mXanderParams.mCheckedItem = checkedItem;
            mXanderParams.mLabelColumn = labelColumn;
            mXanderParams.mIsSingleChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of
         * the selected item via the supplied listener. The list will have a check mark displayed to
         * the right of the text for the checked item. Clicking on an item in the list will not
         * dismiss the dialog. Clicking on a button will dismiss the dialog.
         *
         * @param items       the items to be displayed.
         * @param checkedItem specifies which item is checked. If -1 no items are checked.
         * @param listener    notified when an item on the list is clicked. The dialog will not be
         *                    dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                    button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setSingleChoiceItems(CharSequence[] items, int checkedItem, final OnClickListener listener) {
            mXanderParams.mItems = items;
            mXanderParams.mOnClickListener = listener;
            mXanderParams.mCheckedItem = checkedItem;
            mXanderParams.mIsSingleChoice = true;
            return this;
        }

        /**
         * Set a list of items to be displayed in the dialog as the content, you will be notified of
         * the selected item via the supplied listener. The list will have a check mark displayed to
         * the right of the text for the checked item. Clicking on an item in the list will not
         * dismiss the dialog. Clicking on a button will dismiss the dialog.
         *
         * @param adapter     The {@link ListAdapter} to supply the list of items
         * @param checkedItem specifies which item is checked. If -1 no items are checked.
         * @param listener    notified when an item on the list is clicked. The dialog will not be
         *                    dismissed when an item is clicked. It will only be dismissed if clicked on a
         *                    button, if no buttons are supplied it's up to the user to dismiss the dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setSingleChoiceItems(ListAdapter adapter, int checkedItem, final OnClickListener listener) {
            mXanderParams.mAdapter = adapter;
            mXanderParams.mOnClickListener = listener;
            mXanderParams.mCheckedItem = checkedItem;
            mXanderParams.mIsSingleChoice = true;
            return this;
        }

        /**
         * Sets a listener to be invoked when an item in the list is selected.
         *
         * @param listener The listener to be invoked.
         * @return This Builder object to allow for chaining of calls to set methods
         * @see AdapterView#setOnItemSelectedListener(android.widget.AdapterView.OnItemSelectedListener)
         */
        public Builder setOnItemSelectedListener(final AdapterView.OnItemSelectedListener listener) {
            mXanderParams.mOnItemSelectedListener = listener;
            return this;
        }

        /**
         * Set a custom view to be the contents of the Dialog. If the supplied view is an instance
         * of a {@link ListView} the light background will be used.
         *
         * @param view The view to use as the contents of the Dialog.
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setView(View view) {
            mXanderParams.mView = view;
            mXanderParams.mViewSpacingSpecified = false;
            return this;
        }

        /**
         * Set a custom view to be the contents of the Dialog, specifying the
         * spacing to appear around that view. If the supplied view is an
         * instance of a {@link ListView} the light background will be used.
         *
         * @param view              The view to use as the contents of the Dialog.
         * @param viewSpacingLeft   Spacing between the left edge of the view and
         *                          the dialog frame
         * @param viewSpacingTop    Spacing between the top edge of the view and
         *                          the dialog frame
         * @param viewSpacingRight  Spacing between the right edge of the view
         *                          and the dialog frame
         * @param viewSpacingBottom Spacing between the bottom edge of the view
         *                          and the dialog frame
         * @return This Builder object to allow for chaining of calls to set
         * methods
         * <p/>
         * <p/>
         * This is currently hidden because it seems like people should just
         * be able to put padding around the view.
         * @hide
         */
        public Builder setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
            mXanderParams.mView = view;
            mXanderParams.mViewSpacingSpecified = true;
            mXanderParams.mViewSpacingLeft = viewSpacingLeft;
            mXanderParams.mViewSpacingTop = viewSpacingTop;
            mXanderParams.mViewSpacingRight = viewSpacingRight;
            mXanderParams.mViewSpacingBottom = viewSpacingBottom;
            return this;
        }

        /**
         * Sets the Dialog to use the inverse background, regardless of what the
         * contents is.
         *
         * @param useInverseBackground Whether to use the inverse background
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setInverseBackgroundForced(boolean useInverseBackground) {
            mXanderParams.mForceInverseBackground = useInverseBackground;
            return this;
        }

        /**
         * @hide
         */
        public Builder setRecycleOnMeasureEnabled(boolean enabled) {
            mXanderParams.mRecycleOnMeasure = enabled;
            return this;
        }

        /**
         * Creates a {@link XanderPanel} with the arguments supplied to this builder. It does not
         * {@link Dialog#show()} the dialog. This allows the user to do any extra processing
         * before displaying the dialog. Use {@link #show()} if you don't have any other processing
         * to do and want this to be created and displayed.
         */
        public XanderPanel create() {
            final XanderPanel xanderPanel = new XanderPanel(mXanderParams.mContext);
            mXanderParams.apply(xanderPanel.panelController);
            xanderPanel.setContentView(xanderPanel.panelController.getParentView());
            xanderPanel.mCancelable = mXanderParams.mCancelable;
            return xanderPanel;
        }

        /**
         * Creates a {@link XanderPanel} with the arguments supplied to this builder and
         * {@link Dialog#show()}'s the dialog.
         */
        public XanderPanel show() {
            XanderPanel xanderPanel = create();
            xanderPanel.show();

            xanderPanel.getWindow().getDecorView().setPadding(0,0,0,0);

            return xanderPanel;
        }
    }
}