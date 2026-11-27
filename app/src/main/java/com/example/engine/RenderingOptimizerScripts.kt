package com.example.engine

/**
 * Lightweight, non-intrusive client-side JS/CSS scripts for rendering speed optimization,
 * cosmetic ad/cookie banner hiding, algorithmic Force Dark web rendering, and Clean Reader View.
 */
object RenderingOptimizerScripts {

    /**
     * Optimizes CSS painting and image decoding:
     * - Applies loading="lazy" & decoding="async" to below-the-fold images
     * - Hints content-visibility: auto on heavy semantic sections for faster layout/scroll
     * - Removes empty ad iframes in Turbo Mode
     */
    fun buildPerformanceScript(
        lazyImages: Boolean,
        cssPerfOpt: Boolean,
        turboMode: Boolean
    ): String {
        return """
            (function() {
              try {
                if (${lazyImages}) {
                  var vh = window.innerHeight || 800;
                  var imgs = document.querySelectorAll('img');
                  for (var i = 0; i < imgs.length; i++) {
                    var img = imgs[i];
                    if (!img.getAttribute('decoding')) img.setAttribute('decoding', 'async');
                    var rect = img.getBoundingClientRect();
                    if (rect.top > vh * 1.25 && !img.getAttribute('loading')) {
                      img.setAttribute('loading', 'lazy');
                    }
                  }
                }
                if (${cssPerfOpt} && !document.getElementById('swiftweb-perf-css')) {
                  var style = document.createElement('style');
                  style.id = 'swiftweb-perf-css';
                  style.textContent = 'article, section, .comment, footer { content-visibility: auto; contain-intrinsic-size: auto 320px; }';
                  (document.head || document.documentElement).appendChild(style);
                }
                if (${turboMode}) {
                  var frames = document.querySelectorAll('iframe[src*="ad"], iframe[id*="google_ads"], iframe[class*="ad-"]');
                  for (var j = 0; j < frames.length; j++) {
                    frames[j].remove();
                  }
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Brave-grade YouTube Ad-Blocker Scriptlet (works on both m.youtube.com & www.youtube.com):
     * 1) Prunes adPlacements, playerAds, adSlots, and promoted feed renderers out of
     *    ytInitialPlayerResponse, ytInitialData, fetch(), and XMLHttpRequest responses
     *    so pre-roll, mid-roll, and post-roll video ads never even load.
     * 2) Hides all mobile & desktop YouTube feed/banner/overlay/companion ads via CSS.
     * 3) Runs a zero-delay live player failsafe that mutes, seeks past, and auto-clicks Skip
     *    if any inline cold-load ad slipped in before early injection, then cleanly restores
     *    original volume & playback speed the instant main content resumes.
     * 4) Neutralizes YouTube's anti-adblock enforcement dialogs automatically.
     */
    fun buildYouTubeShieldScript(hideAds: Boolean, hideAnnoyances: Boolean): String {
        if (!hideAds && !hideAnnoyances) return ""
        val ytCss = buildString {
            if (hideAds) {
                append(
                    listOf(
                        "ytm-promoted-sparkles-web-renderer",
                        "ytm-promoted-sparkles-text-search-renderer",
                        "ytm-promoted-video-renderer",
                        "ytm-ad-slot-renderer",
                        "ytm-in-feed-ad-layout-renderer",
                        "ytm-companion-slot",
                        "ytm-banner-promo-renderer",
                        "ytm-statement-banner-renderer",
                        "ad-slot-renderer",
                        "ytd-ad-slot-renderer",
                        "ytd-in-feed-ad-layout-renderer",
                        "ytd-banner-promo-renderer",
                        "ytd-promoted-sparkles-web-renderer",
                        "ytd-promoted-sparkles-text-search-renderer",
                        "ytd-promoted-video-renderer",
                        "ytd-compact-promoted-video-renderer",
                        "ytd-display-ad-renderer",
                        "ytd-companion-slot-renderer",
                        "ytd-action-companion-ad-renderer",
                        "ytd-player-legacy-desktop-watch-ads-renderer",
                        "ytd-rich-item-renderer:has(ytd-ad-slot-renderer)",
                        "ytd-rich-section-renderer:has(ytd-statement-banner-renderer)",
                        "#player-ads",
                        "#masthead-ad",
                        ".ytp-ad-module",
                        ".ytp-ad-overlay-container",
                        ".ytp-ad-player-overlay",
                        ".ytp-ad-player-overlay-layout",
                        ".ytp-ad-image-overlay",
                        ".ytp-ad-text-overlay",
                        ".ytp-ad-action-interstitial",
                        ".video-ads",
                        "ytd-enforcement-message-view-model",
                        "yt-enforcement-message-view-model"
                    ).joinToString(", ")
                )
            }
            if (hideAnnoyances) {
                if (isNotEmpty()) append(", ")
                append(
                    listOf(
                        "ytm-mealbar-promo-renderer",
                        "yt-mealbar-promo-renderer",
                        "ytd-mealbar-promo-renderer",
                        "yt-upsell-dialog-renderer",
                        "tp-yt-paper-dialog:has(ytd-enforcement-message-view-model)",
                        "tp-yt-paper-dialog:has(yt-mealbar-promo-renderer)"
                    ).joinToString(", ")
                )
            }
            append(
                " { display: none !important; visibility: hidden !important; height: 0 !important; " +
                    "min-height: 0 !important; max-height: 0 !important; opacity: 0 !important; pointer-events: none !important; }"
            )
        }
        val escapedCss = ytCss.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
              try {
                var host = (window.location && window.location.hostname || '').toLowerCase();
                if (host.indexOf('youtube.com') === -1 && host.indexOf('youtu.be') === -1) return;

                var styleId = 'fevro-yt-shield-css';
                var styleEl = document.getElementById(styleId);
                if (!styleEl) {
                  styleEl = document.createElement('style');
                  styleEl.id = styleId;
                  (document.head || document.documentElement).appendChild(styleEl);
                }
                styleEl.textContent = '$escapedCss';

                if (window.__fevroYtShieldInstalled) return;
                window.__fevroYtShieldInstalled = true;

                var hideAds = $hideAds;
                var AD_OBJECT_KEYS = {
                  adPlacements: true,
                  playerAds: true,
                  adSlots: true,
                  adBreakHeartbeatParams: true,
                  adBreakParams: true,
                  adSafetyReason: true
                };
                var AD_RENDERER_KEYS = [
                  'adSlotRenderer',
                  'promotedSparklesWebRenderer',
                  'promotedSparklesTextSearchRenderer',
                  'promotedVideoRenderer',
                  'compactPromotedVideoRenderer',
                  'inFeedAdLayoutRenderer',
                  'bannerPromoRenderer',
                  'statementBannerRenderer',
                  'mealbarPromoRenderer',
                  'carouselAdRenderer',
                  'displayAdRenderer',
                  'actionCompanionAdRenderer',
                  'playerLegacyDesktopWatchAdsRenderer',
                  'enforcementMessageViewModel'
                ];

                function reportBlocked(tag) {
                  try {
                    if (window.SwiftShieldBridge && window.SwiftShieldBridge.onAdBlocked) {
                      window.SwiftShieldBridge.onAdBlocked(tag || 'youtube-ad');
                    }
                  } catch (e) {}
                }

                function isAdItem(item) {
                  if (!item || typeof item !== 'object') return false;
                  for (var i = 0; i < AD_RENDERER_KEYS.length; i++) {
                    if (item[AD_RENDERER_KEYS[i]]) return true;
                  }
                  if (item.richItemRenderer && item.richItemRenderer.content && item.richItemRenderer.content.adSlotRenderer) {
                    return true;
                  }
                  if (item.itemSectionRenderer && Array.isArray(item.itemSectionRenderer.contents)) {
                    var c = item.itemSectionRenderer.contents;
                    if (c.length === 1 && isAdItem(c[0])) return true;
                  }
                  return false;
                }

                function pruneYouTubeData(node, depth) {
                  if (!hideAds || !node || typeof node !== 'object' || depth > 14) return 0;
                  var removed = 0;
                  if (Array.isArray(node)) {
                    for (var i = node.length - 1; i >= 0; i--) {
                      if (isAdItem(node[i])) {
                        node.splice(i, 1);
                        removed++;
                      } else if (node[i] && typeof node[i] === 'object') {
                        removed += pruneYouTubeData(node[i], depth + 1);
                      }
                    }
                    return removed;
                  }
                  var keys = Object.keys(node);
                  for (var k = 0; k < keys.length; k++) {
                    var key = keys[k];
                    if (AD_OBJECT_KEYS[key]) {
                      if (Array.isArray(node[key])) {
                        if (node[key].length > 0) removed += node[key].length;
                        node[key] = [];
                      } else if (node[key] != null) {
                        delete node[key];
                        removed++;
                      }
                      continue;
                    }
                    if (key === 'auxiliaryUi' && node[key] && node[key].messageRenderers && node[key].messageRenderers.bkaEnforcementMessageViewModel) {
                      delete node[key].messageRenderers.bkaEnforcementMessageViewModel;
                      removed++;
                      continue;
                    }
                    var val = node[key];
                    if (val && typeof val === 'object') {
                      removed += pruneYouTubeData(val, depth + 1);
                    }
                  }
                  return removed;
                }

                function trapGlobalProp(propName) {
                  try {
                    var currentVal = window[propName];
                    if (currentVal && typeof currentVal === 'object') {
                      if (pruneYouTubeData(currentVal, 0) > 0) reportBlocked('youtube-initial-' + propName);
                    }
                    var desc = Object.getOwnPropertyDescriptor(window, propName);
                    if (desc && desc.configurable === false) return;
                    Object.defineProperty(window, propName, {
                      configurable: true,
                      enumerable: true,
                      get: function() { return currentVal; },
                      set: function(v) {
                        if (v && typeof v === 'object') {
                          if (pruneYouTubeData(v, 0) > 0) reportBlocked('youtube-set-' + propName);
                        }
                        currentVal = v;
                      }
                    });
                  } catch (e) {}
                }

                if (hideAds) {
                  trapGlobalProp('ytInitialPlayerResponse');
                  trapGlobalProp('ytInitialData');

                  // Hook fetch() for SPA YouTube player/browse/next/search/reel API calls
                  if (window.fetch && !window.fetch.__fevroWrapped) {
                    var origFetch = window.fetch;
                    var wrappedFetch = function(input, init) {
                      var urlStr = '';
                      try {
                        urlStr = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                      } catch (e) {}
                      var isYtApi = urlStr.indexOf('/youtubei/v1/player') !== -1 ||
                        urlStr.indexOf('/youtubei/v1/next') !== -1 ||
                        urlStr.indexOf('/youtubei/v1/browse') !== -1 ||
                        urlStr.indexOf('/youtubei/v1/search') !== -1 ||
                        urlStr.indexOf('/youtubei/v1/reel') !== -1 ||
                        urlStr.indexOf('/youtubei/v1/get_watch') !== -1;

                      if (!isYtApi) {
                        return origFetch.apply(this, arguments);
                      }
                      return origFetch.apply(this, arguments).then(function(resp) {
                        try {
                          if (!resp || !resp.ok) return resp;
                          var ct = (resp.headers && resp.headers.get('content-type')) || '';
                          if (ct.indexOf('json') === -1) return resp;
                          return resp.clone().text().then(function(txt) {
                            try {
                              var data = JSON.parse(txt);
                              var pruned = pruneYouTubeData(data, 0);
                              if (pruned > 0) {
                                reportBlocked('youtube-api-player');
                                return new Response(JSON.stringify(data), {
                                  status: resp.status,
                                  statusText: resp.statusText,
                                  headers: resp.headers
                                });
                              }
                            } catch (e) {}
                            return resp;
                          }).catch(function() { return resp; });
                        } catch (e) {
                          return resp;
                        }
                      });
                    };
                    wrappedFetch.__fevroWrapped = true;
                    window.fetch = wrappedFetch;
                  }

                  // Hook XMLHttpRequest for legacy/mobile YouTube API responses
                  if (window.XMLHttpRequest && !window.XMLHttpRequest.prototype.__fevroWrapped) {
                    var origOpen = XMLHttpRequest.prototype.open;
                    var respTextDesc = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'responseText');
                    var respDesc = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype, 'response');
                    XMLHttpRequest.prototype.open = function(method, url) {
                      this.__fevroUrl = typeof url === 'string' ? url : '';
                      return origOpen.apply(this, arguments);
                    };
                    if (respTextDesc && respTextDesc.get) {
                      Object.defineProperty(XMLHttpRequest.prototype, 'responseText', {
                        configurable: true,
                        get: function() {
                          var raw = respTextDesc.get.call(this);
                          var u = this.__fevroUrl || '';
                          if (raw && (u.indexOf('/youtubei/v1/player') !== -1 || u.indexOf('/youtubei/v1/next') !== -1 || u.indexOf('/youtubei/v1/browse') !== -1)) {
                            try {
                              var obj = JSON.parse(raw);
                              if (pruneYouTubeData(obj, 0) > 0) {
                                reportBlocked('youtube-xhr-player');
                                return JSON.stringify(obj);
                              }
                            } catch (e) {}
                          }
                          return raw;
                        }
                      });
                    }
                    if (respDesc && respDesc.get) {
                      Object.defineProperty(XMLHttpRequest.prototype, 'response', {
                        configurable: true,
                        get: function() {
                          var raw = respDesc.get.call(this);
                          var u = this.__fevroUrl || '';
                          if (raw && (u.indexOf('/youtubei/v1/player') !== -1 || u.indexOf('/youtubei/v1/next') !== -1 || u.indexOf('/youtubei/v1/browse') !== -1)) {
                            if (typeof raw === 'string') {
                              try {
                                var obj = JSON.parse(raw);
                                if (pruneYouTubeData(obj, 0) > 0) {
                                  reportBlocked('youtube-xhr-response');
                                  return JSON.stringify(obj);
                                }
                              } catch (e) {}
                            } else if (typeof raw === 'object') {
                              if (pruneYouTubeData(raw, 0) > 0) reportBlocked('youtube-xhr-obj');
                            }
                          }
                          return raw;
                        }
                      });
                    }
                    XMLHttpRequest.prototype.__fevroWrapped = true;
                  }
                }

                // Zero-delay Live Player Failsafe (skips any cold-load ad & restores audio/speed cleanly)
                var savedMutedState = null;
                var wasInAd = false;
                var lastSkipReportTs = 0;

                function neutralizeLiveYouTubeAds() {
                  try {
                    if (!hideAds) return;
                    // Dismiss anti-adblock enforcement popup if YouTube injects one
                    var enforce = document.querySelector('ytd-enforcement-message-view-model, yt-enforcement-message-view-model');
                    if (enforce) {
                      var dlg = enforce.closest('tp-yt-paper-dialog') || enforce.parentElement;
                      if (dlg) dlg.remove();
                      var backdrop = document.querySelector('tp-yt-iron-overlay-backdrop');
                      if (backdrop) backdrop.remove();
                      var mainVid = document.querySelector('video.html5-main-video, video');
                      if (mainVid && mainVid.paused) {
                        mainVid.play().catch(function() {});
                      }
                    }

                    var player = document.querySelector('#movie_player, .html5-video-player');
                    var video = player ? player.querySelector('video') : document.querySelector('video.html5-main-video, video');
                    var adShowing = Boolean(
                      (player && (player.classList.contains('ad-showing') || player.classList.contains('ad-interrupting'))) ||
                      document.querySelector('.ytp-ad-player-overlay, .ytp-ad-player-overlay-layout, .ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button')
                    );

                    if (adShowing) {
                      if (!wasInAd && video) {
                        savedMutedState = video.muted;
                      }
                      wasInAd = true;

                      var now = Date.now();
                      if (now - lastSkipReportTs > 1500) {
                        lastSkipReportTs = now;
                        reportBlocked('youtube-video-ad-skip');
                      }

                      // Click any available skip or overlay close buttons
                      var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                        'button.ytp-ad-skip-button-modern, .ytp-ad-overlay-close-button, button[id^="skip-button"]'
                      );
                      for (var s = 0; s < skipButtons.length; s++) {
                        try { skipButtons[s].click(); } catch (e) {}
                      }

                      if (video) {
                        video.muted = true;
                        if (isFinite(video.duration) && video.duration > 0.2) {
                          video.currentTime = video.duration;
                        } else {
                          video.playbackRate = 16;
                        }
                        if (video.paused) {
                          video.play().catch(function() {});
                        }
                      }
                    } else if (wasInAd) {
                      wasInAd = false;
                      if (video) {
                        if (video.playbackRate > 4) video.playbackRate = 1;
                        if (savedMutedState !== null) {
                          video.muted = savedMutedState;
                          savedMutedState = null;
                        }
                      }
                    }
                  } catch (e) {}
                }

                neutralizeLiveYouTubeAds();
                setInterval(neutralizeLiveYouTubeAds, 220);

                if (window.MutationObserver && document.documentElement) {
                  var ytMutScheduled = false;
                  var ytObserver = new MutationObserver(function() {
                    if (ytMutScheduled) return;
                    ytMutScheduled = true;
                    setTimeout(function() {
                      ytMutScheduled = false;
                      neutralizeLiveYouTubeAds();
                    }, 90);
                  });
                  ytObserver.observe(document.documentElement, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ['class']
                  });
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Two-stage cosmetic ad hiding, empty ad-slot collapse, anti-adblock defusal,
     * popup guard, and intrusive cookie-banner suppressor:
     * 1) Injects an immediate high-specificity stylesheet covering major ad networks, sponsored feeds,
     *    sticky mobile ad bars, and GDPR/consent overlays.
     * 2) Defuses common adblock detection flags so news sites render cleanly without nagging walls.
     * 3) Installs a throttled MutationObserver to collapse newly-injected dynamic ad slots,
     *    empty ad iframes/wrappers, and unlock page scrolling if a hidden consent modal froze body scroll.
     */
    fun buildCosmeticBlockingScript(
        hideAds: Boolean,
        hideAnnoyances: Boolean,
        blockPopups: Boolean = true
    ): String {
        val selectors = mutableListOf<String>()
        if (hideAds) {
            selectors.addAll(
                listOf(
                    ".adsbygoogle",
                    "ins.adsbygoogle",
                    "amp-ad",
                    "amp-auto-ads",
                    "amp-sticky-ad",
                    "iframe[id^='google_ads_iframe']",
                    "div[id^='google_ads_iframe']",
                    "div[id^='div-gpt-ad']",
                    "[id*='taboola']",
                    "[class*='taboola']",
                    "[id*='outbrain']",
                    "[class*='outbrain']",
                    "[id*='mgid']",
                    "[class*='mgid']",
                    "[id*='revcontent']",
                    "[class*='revcontent']",
                    "[id*='zergnet']",
                    "[class*='zergnet']",
                    "[class*='dianomi']",
                    ".ad-banner",
                    ".ad-container",
                    ".ad-slot",
                    ".ad-wrapper",
                    ".ad-placeholder",
                    ".ad-leaderboard",
                    ".ad-sticky",
                    ".sticky-ad",
                    ".mobile-ad",
                    ".banner-ad",
                    ".sponsored-content",
                    ".sponsored-post",
                    ".native-ad",
                    ".promoted-link",
                    ".adthrive-ad",
                    ".mediavine-ad",
                    ".ezoic-ad",
                    "shreddit-ad-post",
                    "div[data-ad-unit]",
                    "div[data-ad-slot]",
                    "div[data-google-query-id]",
                    "div[data-testid='ad-slot']",
                    "aside[aria-label*='Advertisement']",
                    "section[aria-label*='Advertisement']",
                    "iframe[src*='doubleclick.net']",
                    "iframe[src*='googlesyndication.com']",
                    "iframe[src*='amazon-adsystem.com']",
                    "iframe[src*='criteo.com']",
                    "iframe[src*='taboola.com']",
                    "iframe[src*='outbrain.com']",
                    "iframe[src*='adnxs.com']",
                    "iframe[src*='pubmatic.com']"
                )
            )
        }
        if (hideAnnoyances) {
            selectors.addAll(
                listOf(
                    "#onetrust-banner-sdk",
                    "#onetrust-consent-sdk",
                    ".onetrust-pc-dark-filter",
                    "#CybotCookiebotDialog",
                    "#CybotCookiebotDialogBodyUnderlay",
                    ".cookie-banner",
                    ".cookie-consent",
                    ".cookie-notice",
                    "#cookie-notice",
                    "#cookie-law-info-bar",
                    ".cc-window",
                    ".cc-banner",
                    ".fc-consent-root",
                    ".fc-dialog-overlay",
                    ".fc-ab-root",
                    "#didomi-host",
                    "#didomi-popup",
                    ".qc-cmp2-container",
                    "#qc-cmp2-container",
                    "#usercentrics-root",
                    ".truste_overlay",
                    ".truste_box_overlay",
                    "#sp_message_container_",
                    "[id^='sp_message_container']",
                    "[class*='cookie-modal']",
                    "[class*='gdpr-banner']",
                    "[aria-label='cookieconsent']",
                    "[aria-label*='Cookie Consent']",
                    "[role='dialog'][aria-label*='cookie' i]"
                )
            )
        }
        if (selectors.isEmpty()) return ""
        val css = selectors.joinToString(", ") +
            " { display: none !important; visibility: hidden !important; opacity: 0 !important; " +
            "height: 0 !important; min-height: 0 !important; max-height: 0 !important; " +
            "margin: 0 !important; padding: 0 !important; border: 0 !important; overflow: hidden !important; pointer-events: none !important; }"
        val escaped = css.replace("\\", "\\\\").replace("'", "\\'")
        return """
            (function() {
              try {
                var styleId = 'swiftweb-cosmetic-css';
                var styleEl = document.getElementById(styleId);
                if (!styleEl) {
                  styleEl = document.createElement('style');
                  styleEl.id = styleId;
                  (document.head || document.documentElement).appendChild(styleEl);
                }
                styleEl.textContent = '$escaped';

                var hideAds = $hideAds;
                var hideAnnoyances = $hideAnnoyances;
                var blockPopups = $blockPopups;

                // Defuse common anti-adblock detector globals safely
                if (hideAds && !window.__fevroAntiDetectInstalled) {
                  window.__fevroAntiDetectInstalled = true;
                  try { window.canRunAds = true; } catch (e) {}
                  try { window.isAdBlockActive = false; } catch (e) {}
                  try { window.adblock = false; } catch (e) {}
                  try {
                    if (!window.adsbygoogle) {
                      window.adsbygoogle = { loaded: true, push: function() {} };
                    }
                  } catch (e) {}
                }

                // Script-level popunder / ad-popup guard for window.open
                if (blockPopups && window.open && !window.open.__fevroPopupGuard) {
                  var nativeOpen = window.open;
                  var guardedOpen = function(url) {
                    try {
                      var u = (url || '').toString().toLowerCase();
                      if (!u || u === 'about:blank' ||
                          u.indexOf('popads') !== -1 || u.indexOf('popcash') !== -1 ||
                          u.indexOf('propeller') !== -1 || u.indexOf('adclick') !== -1 ||
                          u.indexOf('doubleclick') !== -1 || u.indexOf('popunder') !== -1 ||
                          u.indexOf('clickadu') !== -1 || u.indexOf('exoclick') !== -1) {
                        if (window.SwiftShieldBridge && window.SwiftShieldBridge.onAdBlocked) {
                          window.SwiftShieldBridge.onAdBlocked('popup-window');
                        }
                        return null;
                      }
                    } catch (e) {}
                    return nativeOpen.apply(this, arguments);
                  };
                  guardedOpen.__fevroPopupGuard = true;
                  window.open = guardedOpen;
                }

                function collapseSlots() {
                  try {
                    if (hideAds) {
                      var adFrames = document.querySelectorAll('iframe[src*="doubleclick"], iframe[src*="googlesyndication"], iframe[src*="adnxs"], iframe[src*="criteo"], iframe[id*="google_ads"], ins.adsbygoogle, div[id^="div-gpt-ad"], amp-ad, amp-sticky-ad');
                      for (var i = 0; i < adFrames.length; i++) {
                        var el = adFrames[i];
                        var parent = el.parentElement;
                        el.style.setProperty('display', 'none', 'important');
                        el.style.setProperty('height', '0px', 'important');
                        if (parent && parent.tagName !== 'BODY' && parent.tagName !== 'MAIN' && parent.tagName !== 'ARTICLE') {
                          var cls = (parent.className || '').toString().toLowerCase();
                          var pid = (parent.id || '').toString().toLowerCase();
                          if ((cls.indexOf('ad') !== -1 || pid.indexOf('ad') !== -1 || cls.indexOf('sponsor') !== -1) && (parent.innerText || '').trim().length < 24) {
                            parent.style.setProperty('display', 'none', 'important');
                            parent.style.setProperty('height', '0px', 'important');
                            parent.style.setProperty('min-height', '0px', 'important');
                            parent.style.setProperty('margin', '0px', 'important');
                            parent.style.setProperty('padding', '0px', 'important');
                          }
                        }
                      }
                    }
                    if (hideAnnoyances) {
                      var body = document.body;
                      var docEl = document.documentElement;
                      if (body && (body.style.overflow === 'hidden' || body.classList.contains('didomi-popup-open') || body.classList.contains('fc-dialog-open'))) {
                        body.style.setProperty('overflow', 'auto', 'important');
                        body.style.setProperty('position', 'static', 'important');
                      }
                      if (docEl && docEl.style.overflow === 'hidden') {
                        docEl.style.setProperty('overflow', 'auto', 'important');
                      }
                    }
                  } catch (e) {}
                }

                collapseSlots();

                if (!window.__swiftShieldObserver && window.MutationObserver && document.documentElement) {
                  var scheduled = false;
                  window.__swiftShieldObserver = new MutationObserver(function() {
                    if (scheduled) return;
                    scheduled = true;
                    setTimeout(function() {
                      scheduled = false;
                      collapseSlots();
                    }, 300);
                  });
                  window.__swiftShieldObserver.observe(document.documentElement, { childList: true, subtree: true });
                }
              } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Toggles clean algorithmic dark styling on bright web pages while preserving media elements.
     */
    fun buildForceDarkScript(enabled: Boolean): String {
        return if (enabled) {
            """
            (function() {
              try {
                if (document.getElementById('swiftweb-force-dark')) return;
                var s = document.createElement('style');
                s.id = 'swiftweb-force-dark';
                s.textContent = 'html { filter: invert(0.92) hue-rotate(180deg) !important; background: #0B101B !important; } ' +
                  'img, video, picture, canvas, svg, [style*="background-image"] { filter: invert(1) hue-rotate(180deg) !important; }';
                (document.head || document.documentElement).appendChild(s);
              } catch (e) {}
            })();
            """.trimIndent()
        } else {
            """
            (function() {
              var el = document.getElementById('swiftweb-force-dark');
              if (el) el.remove();
            })();
            """.trimIndent()
        }
    }

    /**
     * Fast DOM probe that checks whether the active web page looks like a readable long-form article
     * so the omnibox can highlight the one-tap Reading Mode pill.
     */
    val READER_ARTICLE_DETECT_JS: String = """
        (function() {
          try {
            if (!document.body) return false;
            var articleRoot = document.querySelector('article, [role="main"] article, main article, .article-body, .post-content, .entry-content, .story-body');
            if (articleRoot) {
              var ap = articleRoot.querySelectorAll('p');
              var alen = 0;
              for (var i = 0; i < ap.length; i++) {
                if (ap[i].closest('nav, header, footer, aside')) continue;
                alen += (ap[i].innerText || '').trim().length;
                if (alen >= 320) return true;
              }
            }
            var ps = document.querySelectorAll('p');
            var goodParagraphs = 0;
            var totalLen = 0;
            for (var j = 0; j < ps.length; j++) {
              var p = ps[j];
              if (p.closest('nav, header, footer, aside, form, .sidebar, .menu, .comments')) continue;
              var t = (p.innerText || '').trim();
              if (t.length >= 60) {
                goodParagraphs++;
                totalLen += t.length;
              }
              if (goodParagraphs >= 3 && totalLen >= 480) return true;
            }
            return false;
          } catch (e) {
            return false;
          }
        })();
    """.trimIndent()

    /**
     * Pure on-device DOM Readability distiller for Reading Mode:
     * 1) Clones document.body so the live page is never mutated.
     * 2) Strips ads, navigation bars, sidebars, related-post rails, social share toolbars,
     *    newsletter popups, comment threads, cookie banners, and hidden nodes.
     * 3) Scores remaining containers by prose density minus link density to isolate the core article.
     * 4) Returns clean structured JSON (headings, paragraphs, quotes, bullet items, code blocks,
     *    author, publish date, hero image URL, and stripped clutter count).
     */
    val READER_EXTRACTOR_JS: String = """
        (function() {
          try {
            var ogTitle = document.querySelector('meta[property="og:title"]');
            var h1El = document.querySelector('article h1, main h1, h1');
            var title = (h1El && h1El.innerText && h1El.innerText.trim().length >= 6 ? h1El.innerText.trim() : '') ||
              (ogTitle && ogTitle.getAttribute('content') ? ogTitle.getAttribute('content').trim() : '') ||
              document.title || 'Untitled Article';

            var authorMeta = document.querySelector('meta[name="author"], meta[property="article:author"], meta[name="parsely-author"]');
            var authorNode = document.querySelector('[rel="author"], .byline, .author-name, [itemprop="author"]');
            var byline = (authorMeta && authorMeta.getAttribute('content')) || (authorNode && authorNode.innerText) || '';
            byline = byline.replace(/^by\s+/i, '').replace(/\s+/g, ' ').trim().slice(0, 80);

            var dateMeta = document.querySelector('meta[property="article:published_time"], meta[name="publish-date"], meta[name="date"]');
            var timeNode = document.querySelector('article time, time[datetime]');
            var rawDate = (dateMeta && dateMeta.getAttribute('content')) || (timeNode && (timeNode.innerText || timeNode.getAttribute('datetime'))) || '';
            var publishedDate = '';
            if (rawDate) {
              var parsedDate = new Date(rawDate);
              if (!isNaN(parsedDate.getTime())) {
                publishedDate = parsedDate.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
              } else {
                publishedDate = rawDate.replace(/\s+/g, ' ').trim().slice(0, 40);
              }
            }

            var siteMeta = document.querySelector('meta[property="og:site_name"]');
            var siteName = (siteMeta && siteMeta.getAttribute('content')) || window.location.hostname.replace(/^www\./, '');

            var leadImageUrl = '';
            var ogImg = document.querySelector('meta[property="og:image"], meta[name="twitter:image"]');
            if (ogImg) {
              var candImg = (ogImg.getAttribute('content') || '').trim();
              if (/^https:\/\//i.test(candImg) && !/doubleclick|googlesyndication|adserver|pixel|spacer|1x1/i.test(candImg)) {
                leadImageUrl = candImg;
              }
            }

            var clone = document.body ? document.body.cloneNode(true) : null;
            if (!clone) {
              return JSON.stringify({ title: title, byline: byline, publishedDate: publishedDate, siteName: siteName, leadImageUrl: leadImageUrl, strippedClutterCount: 0, wordCount: 0, blocks: [] });
            }

            var strippedClutterCount = 0;
            var removeSelectors = [
              'script', 'style', 'noscript', 'iframe', 'svg', 'canvas', 'form', 'button', 'input', 'select', 'textarea',
              'nav', 'header', 'footer', 'aside',
              '[role="navigation"]', '[role="banner"]', '[role="contentinfo"]', '[role="complementary"]', '[role="search"]',
              '.sidebar', '#sidebar', '.side-bar', '.rail', '.right-rail', '.left-rail',
              '.menu', '.nav', '.navbar', '.topbar', '.breadcrumb', '.breadcrumbs',
              '.comments', '#comments', '#disqus_thread', '.comment-section',
              '.related', '.related-posts', '.recommended', '.more-stories', '.read-more',
              '.share', '.social-share', '.share-buttons', '.social-links',
              '.newsletter', '.subscribe', '.signup', '.paywall', '.promo',
              '.ad', '.ads', '.advert', '.advertisement', '.adsbygoogle', '.ad-slot', '.ad-container', '.ad-banner', '.sponsored',
              '[id*="google_ads"]', '[id*="taboola"]', '[class*="taboola"]', '[id*="outbrain"]', '[class*="outbrain"]',
              '[class*="cookie"]', '[id*="cookie"]', '[id*="onetrust"]', '[class*="consent"]', '[aria-hidden="true"]'
            ].join(',');

            var toRemove = Array.from(clone.querySelectorAll(removeSelectors));
            for (var r = 0; r < toRemove.length; r++) {
              if (toRemove[r].parentNode) {
                toRemove[r].parentNode.removeChild(toRemove[r]);
                strippedClutterCount++;
              }
            }

            var negativeHint = /(^|[\s_-])(ad|ads|advert|banner|sponsor|sidebar|side-bar|rail|nav|menu|breadcrumb|social|share|comment|disqus|related|recommend|newsletter|subscribe|promo|popup|modal|cookie|consent|outbrain|taboola|widget|trending|most-popular)($|[\s_-])/i;
            var allDivs = Array.from(clone.querySelectorAll('div, section, ul'));
            for (var d = 0; d < allDivs.length; d++) {
              var el = allDivs[d];
              if (!el.parentNode) continue;
              var metaHint = ((el.className || '') + ' ' + (el.id || '')).toString();
              if (negativeHint.test(metaHint) && el.tagName !== 'ARTICLE' && el.tagName !== 'MAIN') {
                var ownParagraphs = el.querySelectorAll('p').length;
                if (ownParagraphs < 3) {
                  el.parentNode.removeChild(el);
                  strippedClutterCount++;
                }
              }
            }

            function linkDensity(el) {
              var total = (el.innerText || '').trim().length;
              if (total === 0) return 1;
              var links = el.querySelectorAll('a');
              var linkLen = 0;
              for (var k = 0; k < links.length; k++) {
                linkLen += (links[k].innerText || '').trim().length;
              }
              return linkLen / total;
            }

            var candidates = Array.from(clone.querySelectorAll('article, main, [role="main"], .post-content, .article-body, .entry-content, .story-body, section, div'));
            var root = clone;
            var bestScore = 0;
            for (var c = 0; c < candidates.length; c++) {
              var cand = candidates[c];
              var ps = cand.querySelectorAll('p');
              if (ps.length === 0) continue;
              var proseLen = 0;
              var commas = 0;
              for (var pIdx = 0; pIdx < ps.length; pIdx++) {
                var ptxt = (ps[pIdx].innerText || '').trim();
                if (ptxt.length >= 30) {
                  proseLen += ptxt.length;
                  commas += (ptxt.match(/,/g) || []).length;
                }
              }
              if (proseLen < 180) continue;
              var ld = linkDensity(cand);
              if (ld > 0.55) continue;
              var tagBonus = (cand.tagName === 'ARTICLE' || cand.tagName === 'MAIN') ? 1.35 : 1.0;
              var score = (proseLen + commas * 25 + ps.length * 40) * (1 - ld) * tagBonus;
              if (score > bestScore) {
                bestScore = score;
                root = cand;
              }
            }

            if (!leadImageUrl) {
              var firstImg = root.querySelector('img[src^="https://"]');
              if (firstImg) {
                var src = firstImg.getAttribute('src') || '';
                if (!/doubleclick|googlesyndication|pixel|spacer|avatar|icon|logo/i.test(src)) {
                  leadImageUrl = src;
                }
              }
            }

            var nodes = root.querySelectorAll('h2, h3, h4, p, blockquote, li, pre');
            var blocks = [];
            var wordCount = 0;
            var seen = new Set();
            for (var j = 0; j < nodes.length; j++) {
              var node = nodes[j];
              var tag = node.tagName.toLowerCase();
              if (tag === 'p' && node.closest('blockquote, li, pre')) continue;
              var txt = tag === 'pre'
                ? (node.innerText || '').trim()
                : (node.innerText || '').replace(/\s+/g, ' ').trim();
              if (!txt || seen.has(txt)) continue;
              if (txt === title) continue;

              var minLen = (tag === 'h2' || tag === 'h3' || tag === 'h4') ? 4
                : (tag === 'pre' ? 8 : (tag === 'li' ? 14 : 24));
              if (txt.length < minLen) continue;

              // Skip link-heavy navigation remnants inside lists or paragraphs
              if ((tag === 'li' || tag === 'p') && linkDensity(node) > 0.78 && txt.length < 90) continue;

              seen.add(txt);
              var kind = (tag === 'h2' || tag === 'h3' || tag === 'h4') ? 'heading'
                : (tag === 'blockquote' ? 'quote'
                : (tag === 'li' ? 'list_item'
                : (tag === 'pre' ? 'code' : 'paragraph')));
              blocks.push({ kind: kind, text: txt });
              wordCount += txt.split(/\s+/).length;
              if (blocks.length >= 200) break;
            }

            return JSON.stringify({
              title: title,
              byline: byline || '',
              publishedDate: publishedDate || '',
              siteName: siteName || '',
              leadImageUrl: leadImageUrl || '',
              strippedClutterCount: strippedClutterCount,
              wordCount: wordCount,
              blocks: blocks
            });
          } catch (e) {
            return JSON.stringify({ title: document.title || '', byline: '', publishedDate: '', siteName: '', leadImageUrl: '', strippedClutterCount: 0, wordCount: 0, blocks: [] });
          }
        })();
    """.trimIndent()

    /**
     * Observes login forms containing <input type="password"> fields:
     * 1) Notifies native SwiftPasswordBridge when a visible password input is present so
     *    matching local credentials can be offered in the floating autofill bar.
     * 2) Captures username/email + password on form submit, submit-button click, or Enter key
     *    inside a password field and passes them to SwiftPasswordBridge for local encrypted saving.
     */
    val PASSWORD_FORM_OBSERVER_JS: String = """
        (function() {
          try {
            if (!window.SwiftPasswordBridge) return;

            function isVisible(el) {
              if (!el) return false;
              var style = window.getComputedStyle(el);
              if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
              var rect = el.getBoundingClientRect();
              return rect.width > 0 && rect.height > 0;
            }

            function findUsernameField(pwdInput) {
              var scope = pwdInput.form || pwdInput.closest('form') || document;
              var candidates = Array.from(scope.querySelectorAll('input')).filter(function(inp) {
                var t = (inp.type || 'text').toLowerCase();
                if (t !== 'email' && t !== 'text' && t !== 'tel') return false;
                if (inp.disabled || inp.readOnly || !isVisible(inp)) return false;
                var hint = ((inp.name || '') + ' ' + (inp.id || '') + ' ' + (inp.autocomplete || '') + ' ' + (inp.placeholder || '')).toLowerCase();
                if (hint.indexOf('search') !== -1 || hint.indexOf('otp') !== -1 || hint.indexOf('captcha') !== -1) return false;
                return true;
              });
              if (candidates.length === 0) return null;
              var preferred = candidates.find(function(inp) {
                var t = (inp.type || '').toLowerCase();
                var hint = ((inp.name || '') + ' ' + (inp.id || '') + ' ' + (inp.autocomplete || '') + ' ' + (inp.placeholder || '')).toLowerCase();
                return t === 'email' || hint.indexOf('user') !== -1 || hint.indexOf('email') !== -1 || hint.indexOf('login') !== -1 || hint.indexOf('account') !== -1 || hint.indexOf('phone') !== -1;
              });
              if (preferred) return preferred;
              var allInputs = Array.from(scope.querySelectorAll('input'));
              var pwdIdx = allInputs.indexOf(pwdInput);
              for (var i = pwdIdx - 1; i >= 0; i--) {
                if (candidates.indexOf(allInputs[i]) !== -1) return allInputs[i];
              }
              return candidates[0];
            }

            var lastSentSignature = '';
            function reportCredentials(pwdInput) {
              if (!pwdInput) return;
              var password = (pwdInput.value || '');
              if (password.length < 2) return;
              var userInput = findUsernameField(pwdInput);
              var username = userInput ? (userInput.value || '').trim() : '';
              var sig = window.location.hostname + '|' + username + '|' + password;
              if (sig === lastSentSignature) return;
              lastSentSignature = sig;
              try {
                window.SwiftPasswordBridge.onCredentialsSubmitted(window.location.href, username, password);
              } catch (e) {}
            }

            function scanAndNotify() {
              var pwds = Array.from(document.querySelectorAll('input[type="password"]')).filter(function(el) {
                return !el.disabled && isVisible(el);
              });
              if (pwds.length > 0 && !window.__swiftPwdDetectedUrl) {
                window.__swiftPwdDetectedUrl = window.location.href;
                try {
                  window.SwiftPasswordBridge.onPasswordFormDetected(window.location.href);
                } catch (e) {}
              }
            }

            if (!window.__swiftPwdHooksInstalled) {
              window.__swiftPwdHooksInstalled = true;

              document.addEventListener('submit', function(ev) {
                try {
                  var form = ev.target;
                  if (!form || !form.querySelector) return;
                  var pwd = form.querySelector('input[type="password"]');
                  if (pwd && pwd.value) reportCredentials(pwd);
                } catch (e) {}
              }, true);

              document.addEventListener('click', function(ev) {
                try {
                  var target = ev.target && ev.target.closest ? ev.target.closest('button, input[type="submit"], [role="button"]') : null;
                  if (!target) return;
                  var form = target.form || target.closest('form') || document;
                  var pwd = form.querySelector('input[type="password"]');
                  if (pwd && pwd.value && pwd.value.length >= 2) {
                    setTimeout(function() { reportCredentials(pwd); }, 60);
                  }
                } catch (e) {}
              }, true);

              document.addEventListener('keydown', function(ev) {
                try {
                  if (ev.key === 'Enter' && ev.target && ev.target.type === 'password' && ev.target.value) {
                    reportCredentials(ev.target);
                  }
                } catch (e) {}
              }, true);

              if (window.MutationObserver && document.documentElement) {
                var scanScheduled = false;
                var obs = new MutationObserver(function() {
                  if (scanScheduled) return;
                  scanScheduled = true;
                  setTimeout(function() {
                    scanScheduled = false;
                    scanAndNotify();
                  }, 450);
                });
                obs.observe(document.documentElement, { childList: true, subtree: true });
              }
            }

            window.__swiftPwdDetectedUrl = null;
            scanAndNotify();
          } catch (e) {}
        })();
    """.trimIndent()

    /**
     * Safely auto-fills the active page's visible username/email and password fields using the
     * native HTMLInputElement value setter + input/change events so SPA frameworks (React/Vue/Angular)
     * immediately pick up the credentials.
     */
    fun buildPasswordAutofillScript(username: String, password: String): String {
        val safeUser = org.json.JSONObject.quote(username)
        val safePass = org.json.JSONObject.quote(password)
        return """
            (function() {
              try {
                var userVal = $safeUser;
                var passVal = $safePass;

                function setNativeValue(el, val) {
                  if (!el) return;
                  var proto = window.HTMLInputElement && window.HTMLInputElement.prototype;
                  var desc = proto ? Object.getOwnPropertyDescriptor(proto, 'value') : null;
                  if (desc && desc.set) {
                    desc.set.call(el, val);
                  } else {
                    el.value = val;
                  }
                  el.dispatchEvent(new Event('input', { bubbles: true }));
                  el.dispatchEvent(new Event('change', { bubbles: true }));
                }

                function isVisible(el) {
                  if (!el) return false;
                  var st = window.getComputedStyle(el);
                  if (st.display === 'none' || st.visibility === 'hidden') return false;
                  var r = el.getBoundingClientRect();
                  return r.width > 0 && r.height > 0;
                }

                var pwds = Array.from(document.querySelectorAll('input[type="password"]')).filter(function(el) {
                  return !el.disabled && !el.readOnly && isVisible(el);
                });
                var pwdInput = pwds[0] || document.querySelector('input[type="password"]');
                var scope = (pwdInput && (pwdInput.form || pwdInput.closest('form'))) || document;

                var userCandidates = Array.from(scope.querySelectorAll('input')).filter(function(inp) {
                  var t = (inp.type || 'text').toLowerCase();
                  if (t !== 'email' && t !== 'text' && t !== 'tel') return false;
                  if (inp.disabled || inp.readOnly || !isVisible(inp)) return false;
                  var hint = ((inp.name || '') + ' ' + (inp.id || '') + ' ' + (inp.autocomplete || '') + ' ' + (inp.placeholder || '')).toLowerCase();
                  return hint.indexOf('search') === -1 && hint.indexOf('captcha') === -1;
                });

                var userInput = userCandidates.find(function(inp) {
                  var t = (inp.type || '').toLowerCase();
                  var hint = ((inp.name || '') + ' ' + (inp.id || '') + ' ' + (inp.autocomplete || '') + ' ' + (inp.placeholder || '')).toLowerCase();
                  return t === 'email' || hint.indexOf('user') !== -1 || hint.indexOf('email') !== -1 || hint.indexOf('login') !== -1 || hint.indexOf('account') !== -1;
                }) || userCandidates[0];

                var filled = false;
                if (userInput && userVal) {
                  setNativeValue(userInput, userVal);
                  filled = true;
                }
                if (pwdInput && passVal) {
                  setNativeValue(pwdInput, passVal);
                  filled = true;
                }
                return filled;
              } catch (e) {
                return false;
              }
            })();
        """.trimIndent()
    }
}

