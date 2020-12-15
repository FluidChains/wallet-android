package io.certifico.app.ui.home;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

import com.fluidcerts.android.app.databinding.ListItemIssuerBinding;

public class GenericViewHolder extends RecyclerView.ViewHolder {

    public GenericViewHolder(ViewDataBinding binding) {
        super(binding.getRoot());
    }
}