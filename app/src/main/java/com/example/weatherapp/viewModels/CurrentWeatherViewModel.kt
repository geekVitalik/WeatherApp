package com.example.weatherapp.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.weatherapp.data.Exceptions
import com.example.weatherapp.location.Location
import com.example.weatherapp.Utils.Mapper
import com.example.weatherapp.data.WeatherRepository
import com.example.weatherapp.adapters.WeatherPerDay
import com.example.weatherapp.adapters.WeatherPerHour
import com.example.weatherapp.network.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.UnknownHostException


class CurrentWeatherViewModel(application: Application) : AndroidViewModel(
    application
) {
    private val location = Location(application)
    private var _currentWeather = MutableLiveData<WeatherData?>()
    private var _listWeatherPerDay = MutableLiveData<List<WeatherPerDay>?>()
    private var _weatherPerHour = MutableLiveData<WeatherPerHour>()
    private var _isLoading = MutableLiveData<Boolean>()
    private var _exception = MutableLiveData<Exceptions>()

    var currentWeather: LiveData<WeatherData?> = _currentWeather
    var listWeatherPerDay: LiveData<List<WeatherPerDay>?> = _listWeatherPerDay

    var weatherPerHour: LiveData<WeatherPerHour> = _weatherPerHour


    var isLoading: LiveData<Boolean> = _isLoading
    var exception: LiveData<Exceptions> = _exception

    init {
        getLastKnownLocation()
        location.currentLocation.observeForever {
            getWeatherByLocation(it.latitude, it.longitude)
        }
    }

    fun updateWeatherPerHour(weatherPerHour: WeatherPerHour){
        _weatherPerHour.postValue(weatherPerHour)
    }

    fun getLocation() {
        _isLoading.value = true
        _exception.value = Exceptions.noException
        location.getLastLocation()
    }


    private fun getLastKnownLocation() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val result = WeatherRepository.getLastKnownWeather()
                    _currentWeather.postValue(result)
                    _listWeatherPerDay.postValue(Mapper.getWeatherPerDays(result))
                    _weatherPerHour.postValue(Mapper.getWeatherPerDays(result)[0].weatherPerHour[0])
                }
            } catch (ex: Exception) {

            }
        }
    }

    private fun getWeatherByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _exception.value = Exceptions.noException
            try {
                withContext(Dispatchers.IO) {
                    val response = WeatherRepository.getWeatherByCoord(lat, lon)
                    response.isLastKnownLocation = true
                    response.subWeather.forEach {
                        it.isLastKnownLocation = true
                    }
                    WeatherRepository.saveLastKnownLocation(response)
                    _currentWeather.postValue(response)
                    _listWeatherPerDay.postValue(Mapper.getWeatherPerDays(response))
                    _weatherPerHour.postValue(Mapper.getWeatherPerDays(response)[0].weatherPerHour[0])
                }
                _isLoading.value = false
            } catch (ex: UnknownHostException) {
                _exception.value = Exceptions.noInternet
                _isLoading.value = false
            } catch (ex: Throwable) {
                _exception.value = Exceptions.others
                _isLoading.value = false
            }
        }
    }

}


