package nl.tudelft.trustchain.literaturedao.ui

import android.os.Bundle
import android.view.View
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.literaturedao.R
import nl.tudelft.trustchain.literaturedao.databinding.FragmentLiteratureOverviewBinding

class MyLiteratureFragment : BaseFragment(R.layout.fragment_literature_overview) {
    private val binding by viewBinding(FragmentLiteratureOverviewBinding::bind)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoText1.text = "I changed the title!"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
