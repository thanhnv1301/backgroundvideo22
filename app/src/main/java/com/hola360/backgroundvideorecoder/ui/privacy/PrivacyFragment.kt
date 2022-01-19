package com.hola360.backgroundvideorecoder.ui.privacy

import com.hola360.backgroundvideorecoder.R
import com.hola360.backgroundvideorecoder.databinding.FragmentPrivacyBinding
import com.hola360.backgroundvideorecoder.ui.base.basefragment.BaseFragment

class PrivacyFragment :BaseFragment<FragmentPrivacyBinding>() {

    override val layoutId: Int = R.layout.fragment_privacy
    override val showToolbar: Boolean = true
    override val toolbarTitle: String? by lazy {
        if(args.isPrivacy){
            resources.getString(R.string.privacy_policy)
        }else{
            resources.getString(R.string.term_of_service)
        }
    }
    override val menuCode: Int = 0
    private val args: PrivacyFragmentArgs by lazy {
        PrivacyFragmentArgs.fromBundle(requireArguments())
    }

    override fun initView() {
    }

    override fun initViewModel() {

    }
}