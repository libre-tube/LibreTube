package com.github.libretube.helpers
  
  import android.content.Context
  import com.github.libretube.util.NewPipeDownloaderImpl
  import org.schabi.newpipe.extractor.NewPipe
  import org.schabi.newpipe.extractor.ServiceList
  import org.schabi.newpipe.extractor.StreamingService
  import org.schabi.newpipe.extractor.localization.ContentCountry
  import org.schabi.newpipe.extractor.localization.Localization
  
  object NewPipeExtractorInstance {
      val extractor: StreamingService by lazy {
          NewPipe.getService(ServiceList.YouTube.serviceId)
      }   
      
      fun init(context: Context) {
          @Suppress("DEPRECATION")
          val locale = LocaleHelper.getAppLocale()
          val region = PreferenceHelper.getTrendingRegion(context)

          NewPipe.init(
              NewPipeDownloaderImpl(),
              Localization(locale.language, locale.country.ifEmpty { null }),
              ContentCountry(region)
          )
      }
  }
