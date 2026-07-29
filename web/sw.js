"use strict";

var CACHE_NAME = 'reenact-v1';

// The app shell. Versioned assets (app.js?v=..., app.css?v=...) also get cached at
// runtime under their full URLs; these unversioned copies are the offline fallback.
var PRECACHE_URLS = [
	'./',
	'index.html',
	'css/app.css',
	'js/app.js',
	'js/l10n.js',
	'locales/reenact.en-US.properties',
	'audio/shutter.opus',
	'audio/shutter.m4a',
	'img/icons/icon128x128.png',
	'img/icons/icon512x512.png',
	'img/icons/icon-sad512x512.png',
	'img/icons/loading.svg',
	'img/icons/ic_arrow_back_white_48dp.png',
	'img/icons/ic_check_white_48dp.png',
	'img/icons/ic_file_download_white_48dp.png',
	'img/icons/ic_replay_white_48dp.png',
	'img/icons/ic_switch_camera_white_48dp.png',
	'img/icons/share-192.png',
	'img/icons/switch-camera-192.png'
];

self.addEventListener( 'install', function ( event ) {
	event.waitUntil(
		caches.open( CACHE_NAME ).then( function ( cache ) {
			return cache.addAll( PRECACHE_URLS );
		} ).then( function () {
			return self.skipWaiting();
		} )
	);
} );

self.addEventListener( 'activate', function ( event ) {
	event.waitUntil(
		caches.keys().then( function ( keys ) {
			return Promise.all( keys.map( function ( key ) {
				if ( key !== CACHE_NAME ) {
					return caches.delete( key );
				}
			} ) );
		} ).then( function () {
			return self.clients.claim();
		} )
	);
} );

self.addEventListener( 'fetch', function ( event ) {
	if ( event.request.method !== 'GET' ) {
		return;
	}

	if ( event.request.mode === 'navigate' ) {
		// Always try the network for the page itself so that new deployments (and the
		// versioned asset URLs they reference) are picked up immediately.
		event.respondWith(
			fetch( event.request ).then( function ( response ) {
				var copy = response.clone();

				caches.open( CACHE_NAME ).then( function ( cache ) {
					cache.put( event.request, copy );
				} );

				return response;
			} ).catch( function () {
				return caches.match( event.request ).then( function ( cached ) {
					return cached || caches.match( 'index.html' );
				} );
			} )
		);

		return;
	}

	// For everything else: serve from the cache for speed and offline support, while
	// refreshing the cached copy from the network in the background.
	event.respondWith(
		caches.open( CACHE_NAME ).then( function ( cache ) {
			return cache.match( event.request ).then( function ( cached ) {
				var networked = fetch( event.request ).then( function ( response ) {
					if ( response.ok ) {
						cache.put( event.request, response.clone() );
					}

					return response;
				} );

				if ( cached ) {
					networked.catch( function () {
						// Offline; the cached copy already covers us.
					} );

					return cached;
				}

				return networked.catch( function () {
					// Offline and not cached under this exact URL; a copy cached under
					// a different ?v= is better than nothing.
					return cache.match( event.request, { ignoreSearch: true } );
				} );
			} );
		} )
	);
} );
