package com.example.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.utils.AdsManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdViewComposable(
    modifier: Modifier = Modifier,
    adUnitId: String = AdsManager.NATIVE_AD_UNIT_ID
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    val surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                nativeAd?.destroy()
                nativeAd = ad
                isAdLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isAdLoaded = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (isAdLoaded && nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                factory = { ctx ->
                    val nativeAdView = NativeAdView(ctx)

                    val layout = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Top Row: Ad Badge + Icon + Headline + Advertiser
                    val topRow = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 10 }
                    }

                    val badge = TextView(ctx).apply {
                        text = "Ad"
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#3B82F6"))
                        setPadding(10, 4, 10, 4)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { rightMargin = 10 }
                    }
                    topRow.addView(badge)

                    val iconView = ImageView(ctx).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(72, 72).apply {
                            rightMargin = 10
                        }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    nativeAdView.iconView = iconView
                    topRow.addView(iconView)

                    val titleCol = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                    }

                    val headlineView = TextView(ctx).apply {
                        textSize = 15f
                        setTextColor(onSurfaceColor)
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    nativeAdView.headlineView = headlineView
                    titleCol.addView(headlineView)

                    val advertiserView = TextView(ctx).apply {
                        textSize = 12f
                        setTextColor(onSurfaceVariantColor)
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                    }
                    nativeAdView.advertiserView = advertiserView
                    titleCol.addView(advertiserView)

                    topRow.addView(titleCol)
                    layout.addView(topRow)

                    val bodyView = TextView(ctx).apply {
                        textSize = 13f
                        setTextColor(onSurfaceVariantColor)
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 10 }
                    }
                    nativeAdView.bodyView = bodyView
                    layout.addView(bodyView)

                    val mediaView = MediaView(ctx).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            320
                        ).apply { bottomMargin = 10 }
                    }
                    nativeAdView.mediaView = mediaView
                    layout.addView(mediaView)

                    val callToActionView = Button(ctx).apply {
                        textSize = 14f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(primaryColor)
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    nativeAdView.callToActionView = callToActionView
                    layout.addView(callToActionView)

                    nativeAdView.addView(layout)
                    populateNativeAd(nativeAd!!, nativeAdView)
                    nativeAdView
                },
                update = { view ->
                    populateNativeAd(nativeAd!!, view)
                }
            )
        }
    }
}

private fun populateNativeAd(nativeAd: NativeAd, nativeAdView: NativeAdView) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

    if (nativeAd.body == null) {
        nativeAdView.bodyView?.visibility = View.GONE
    } else {
        nativeAdView.bodyView?.visibility = View.VISIBLE
        (nativeAdView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        nativeAdView.callToActionView?.visibility = View.GONE
    } else {
        nativeAdView.callToActionView?.visibility = View.VISIBLE
        (nativeAdView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        nativeAdView.iconView?.visibility = View.GONE
    } else {
        (nativeAdView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        nativeAdView.iconView?.visibility = View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
        nativeAdView.advertiserView?.visibility = View.GONE
    } else {
        (nativeAdView.advertiserView as? TextView)?.text = nativeAd.advertiser
        nativeAdView.advertiserView?.visibility = View.VISIBLE
    }

    nativeAdView.setNativeAd(nativeAd)
}

