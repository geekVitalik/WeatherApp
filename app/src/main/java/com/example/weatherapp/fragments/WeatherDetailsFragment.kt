package com.example.weatherapp.fragments

import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.weatherapp.*
import com.example.weatherapp.adapters.*
import com.example.weatherapp.data.Exceptions
import com.example.weatherapp.data.WeatherPerHour
import com.example.weatherapp.databinding.DetailsWeatherFragmentBinding
import com.example.weatherapp.viewModels.WeatherDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.charts_layout.view.*
import kotlinx.android.synthetic.main.current_weather_fragment.*
import kotlinx.android.synthetic.main.details_weather_bottom.view.*
import javax.inject.Inject

class WeatherDetailsFragment : Fragment() {

    @Inject
    lateinit var weatherViewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: WeatherDetailsViewModel

    private lateinit var binding: DetailsWeatherFragmentBinding
    private lateinit var daysScrollAdapter: DaysScrollAdapter
    private lateinit var chartViewPagerAdapter: ChartViewPagerAdapter
    private lateinit var navController: NavController
    private lateinit var refreshItem: View
    private lateinit var animation: Animation

    private var citiId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        App.get(requireActivity().application).applicationComponent.inject(this)
        viewModel = ViewModelProvider(this, weatherViewModelFactory)
            .get(WeatherDetailsViewModel::class.java)

        navController = NavHostFragment.findNavController(this)
        citiId = WeatherDetailsFragmentArgs.fromBundle(requireArguments()).cityId
        viewModel.getWeatherFromDb(citiId)

        binding = DetailsWeatherFragmentBinding.inflate(inflater, container, false)

        createAdapters()

        setHasOptionsMenu(true)
        return binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewmodel = viewModel
            root.charts_vp.adapter = chartViewPagerAdapter
            root.content_bottom_rv.adapter = daysScrollAdapter
            TabLayoutMediator(root.tabLayout, root.charts_vp) { tab, position ->
                tab.text = getTabName(position)
            }.attach()
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createDataObservers()
        createExceptionObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.refresh_item, menu)
        animation = RotateAnimation(
            0.0f, 360.0f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
            0.5f
        ).apply {
            repeatCount = -1
            duration = 1500
        }

        refreshItem = menu.findItem(R.id.action_refresh).actionView

        refreshItem.setOnClickListener {
            getLastLocation()
        }
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading)
                refreshItem.startAnimation(animation)
            else
                refreshItem.clearAnimation()
        })
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyOptionsMenu() {
        refreshItem.clearAnimation()
        super.onDestroyOptionsMenu()
    }

    private fun getLastLocation() {
        viewModel.refreshWeatherFromInternet()
    }

    private fun createAdapters() {
        chartViewPagerAdapter = ChartViewPagerAdapter(requireContext(), emptyList(), object :
            OnChartItemClickListener {
            override fun onChartItemClick(weatherPerHour: WeatherPerHour) {
                viewModel.updateWeatherPerHour(weatherPerHour)
            }
        })

        daysScrollAdapter = DaysScrollAdapter(
            emptyList(),
            object : OnDaysScrollItemClickListener {
                override fun onItemClick(weatherPerHour: List<WeatherPerHour>) {

                    if (daysScrollAdapter.selectedPosition == 0) {
                        viewModel.updateWeatherPerHour(weatherPerHour[0])
                    } else
                        viewModel.updateWeatherPerHour(weatherPerHour[weatherPerHour.size / 2])

                    chartViewPagerAdapter.data = weatherPerHour
                    chartViewPagerAdapter.notifyDataSetChanged()
                }
            }
        )
    }

    private fun createDataObservers() {
        viewModel.listWeatherPerDay.observe(viewLifecycleOwner, Observer { listWeatherPerDay ->
            if (listWeatherPerDay != null) {
                chartViewPagerAdapter.data = listWeatherPerDay[0].weatherPerHour
                chartViewPagerAdapter.notifyDataSetChanged()
                daysScrollAdapter.values = listWeatherPerDay
                daysScrollAdapter.selectedPosition = 0
                daysScrollAdapter.notifyDataSetChanged()
            }
        })

        viewModel.currentWeather.observe(viewLifecycleOwner, Observer { weather ->
            if (weather != null) {
                requireActivity().toolbar.title = weather.name
            }
        })
    }

    private fun createExceptionObservers() {
        viewModel.exception.observe(viewLifecycleOwner, Observer { exception ->
            if (exception != Exceptions.NoException) {
                Snackbar.make(
                    current_weather_fragment,
                    getString(Exceptions.getNameException(exception)),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun getTabName(position: Int): String{
        return when (position) {
            0 -> {
                getString(R.string.temperature)
            }
            1 -> {
                getString(R.string.precipitation)
            }
            2 -> {
                getString(R.string.wind)
            }
            3 -> {
                getString(R.string.cloudiness)
            }
            else -> ""
        }
    }

}