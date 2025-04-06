package com.example.weatherapplication

import WeatherResponse
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var cityInput: EditText
    private lateinit var fetchButton: Button
    private lateinit var weatherScrollView: ScrollView
    private lateinit var weatherInfoTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var moreDetailsButton: Button

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherApiService = retrofit.create(WeatherApiService::class.java)
    private val apiKey = "b3c224fa517b0783315130dac736777d"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        cityInput = findViewById(R.id.cityInput)
        fetchButton = findViewById(R.id.fetchButton)
        weatherScrollView = findViewById(R.id.weatherScrollView)
        weatherInfoTextView = findViewById(R.id.weatherInfo)
        progressBar = findViewById(R.id.progressBar)
        moreDetailsButton = findViewById(R.id.moreDetailsButton)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchButton.setOnClickListener {
            val city = cityInput.text.toString()
            if (city.isNotEmpty()) {
                progressBar.visibility = View.VISIBLE
                weatherScrollView.visibility = View.GONE
                moreDetailsButton.visibility = View.GONE
                lifecycleScope.launch {
                    try {
                        val weatherResponse = weatherApiService.getCurrentWeather(city, apiKey)
                        val weatherInfo = formatWeatherInfo(weatherResponse)
                        weatherInfoTextView.text = Html.fromHtml(weatherInfo, Html.FROM_HTML_MODE_COMPACT)
                        weatherScrollView.visibility = View.VISIBLE
                        weatherScrollView.alpha = 0f
                        weatherScrollView.animate().alpha(1f).setDuration(500).start()
                        moreDetailsButton.visibility = View.VISIBLE
                        moreDetailsButton.setOnClickListener {
                            showMoreDetails(weatherResponse)
                        }
                    } catch (e: Exception) {
                        weatherInfoTextView.text = "Ошибка при загрузке погоды: ${e.localizedMessage}"
                        weatherScrollView.visibility = View.VISIBLE
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }
            } else {
                weatherInfoTextView.text = "Пожалуйста, введи название города"
                weatherScrollView.visibility = View.VISIBLE
            }
        }
    }

    private fun formatWeatherInfo(response: WeatherResponse): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("ru"))
        val date = Date(response.dt * 1000)
        val sunrise = Date(response.sys.sunrise * 1000)
        val sunset = Date(response.sys.sunset * 1000)

        return """
            <b>${sdf.format(date)}</b>    ${response.name}
            <br><br>
            ${response.weather[0].main}<br>${response.weather[0].description}
            <br><br>
            <big><b>${response.main.temp} °C</b></big><br>
            <b>${response.main.temp_max}°C ↑ / ${response.main.temp_min}°C ↓</b><br><br>
            
            ${response.main.humidity}% Humidity<br>
            ${response.main.pressure} mBar Pressure<br>
            ${response.wind.speed} м/с Wind speed<br><br>
            
            ${SimpleDateFormat("h:mm a", Locale.US).format(sunrise)} Sunrise<br>
            ${SimpleDateFormat("h:mm a", Locale.US).format(sunset)} Sunset<br>
            ${response.clouds.all}% Cloudy
        """.trimIndent()
    }

    private fun showMoreDetails(response: WeatherResponse) {
        val moreInfo = "Дополнительные данные:\n" +
                "Давление: ${response.main.pressure} мБар\n" +
                "Осадки: Нет данных"
        AlertDialog.Builder(this)
            .setTitle("Подробности")
            .setMessage(moreInfo)
            .setPositiveButton("ОК", null)
            .show()
    }
}