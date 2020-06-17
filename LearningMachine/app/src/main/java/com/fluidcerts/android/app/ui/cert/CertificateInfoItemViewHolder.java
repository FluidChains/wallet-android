package com.fluidcerts.android.app.ui.cert;

import android.support.v7.widget.RecyclerView;

import com.fluidcerts.android.app.databinding.CertificateInfoItemBinding;

public class CertificateInfoItemViewHolder extends RecyclerView.ViewHolder {
    private final CertificateInfoItemBinding mBinding;

    public CertificateInfoItemViewHolder(CertificateInfoItemBinding binding) {
        super(binding.getRoot());
        mBinding = binding;
    }

    public void bind(CertificateInfoItemViewModel viewModel) {
        mBinding.setItem(viewModel);
    }
}
