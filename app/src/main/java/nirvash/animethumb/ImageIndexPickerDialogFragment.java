package nirvash.animethumb;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

public class ImageIndexPickerDialogFragment extends DialogFragment {

    private NumberPicker mNumberPicker;

    public static ImageIndexPickerDialogFragment newInstance(String title, int index) {
        ImageIndexPickerDialogFragment frag = new ImageIndexPickerDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putInt("index", index);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.image_index_picker, null);
        mNumberPicker = view.findViewById(R.id.numberPicker);
        mNumberPicker.setMinValue(0);
        mNumberPicker.setMaxValue(10);

        int index = getArguments().getInt("index");
        mNumberPicker.setValue(index);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        String title = getArguments().getString("title");
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AnimeThumbAppWidgetConfigureActivity activity = (AnimeThumbAppWidgetConfigureActivity) getActivity();
                activity.onImageIndexPicked(mNumberPicker.getValue());
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return alertDialogBuilder.create();
    }
}