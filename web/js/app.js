"use strict";

var App = {
	cameraScreens : [ 'capture' ],

	persistentVars : { },

	startup : function () {
		console.log( "App.startup()" );

		App.setOrientation();

		Views.show( 'intro' );
	},

	showScreen : function ( screenId ) {
		console.log( "App.showScreen()" );

		var screens = document.getElementsByClassName( 'app-screen' );

		for ( var i = 0; i < screens.length; i++ ) {
			screens.item( i ).style.display = 'none';
		}

		document.getElementById( 'screen-' + screenId ).style.display = 'block';

		App.persistentVar( 'current-screen', screenId );
	},

	setOrientation : function () {
		console.log( "App.setOrientation()" );

		document.body.setAttribute( 'orientation', screen.mozOrientation );

		Camera.alignViewfinder();
	},

	cache : function ( key, val ) {
		if ( arguments.length === 1 ) {
			return localStorage[key];
		}
		else {
			if ( val === null && typeof val === 'object' ) {
				delete localStorage[key];
			}
			else {
				localStorage[key] = val;
			}
		}
	},
	
	loading : function () {
		document.getElementById( 'loading' ).style.display = 'block';
	},
	
	loaded : function () {
		document.getElementById( 'loading' ).style.display = 'none';
	},
	
	persistentVar : function ( key, val ) {
		if ( arguments.length === 1 ) {
			return App.persistentVars[key];
		}
		else {
			if ( val === null && typeof val === 'object' ) {
				delete App.persistentVars[key];
			}
			else {
				App.persistentVars[key] = val;
			}
		}
	},
};

var Views = {
	viewSpecificTimers : {},

	show : function ( screenId ) {
		App.loaded();
		
		Camera.shutdown();

		for ( var i in Views.viewSpecificTimers ) {
			clearTimeout( Views.viewSpecificTimers[i] );
		}

		function continueView() {
			App.showScreen( screenId );
			
			if ( screenId in Views.viewHandlers ) {
				Views.viewHandlers[screenId]();
			}
			else {
				console.log( "No view handler for " + screenId );
			}
		}

		if ( screenId in Views.preViewHandlers ) {
			Views.preViewHandlers[screenId]();
		}

		continueView();
	},

	preViewHandlers : {
		'capture' : function () {
			document.getElementById( 'original-photo' ).style.visibility = 'hidden';
			
			document.getElementById( 'viewfinder' ).setAttribute( 'class', 'fading' );
		}
	},

	viewHandlers : {
		'intro' : function () {
			App.persistentVar( 'original-photo', null );
		},

		'capture' : function () {
			App.loading();
			
			var photoDataURL = App.persistentVar( 'original-photo-data-url' );
			var originalPhoto = document.getElementById( 'original-photo' );

			originalPhoto.onload = function () {
				originalPhoto.onload = null;

				var realImageWidth = originalPhoto.naturalWidth;
				var realImageHeight = originalPhoto.naturalHeight;

				// Force the new photo to be taken in the same orientation as the old photo.
				var photoIsLandscape = realImageWidth > realImageHeight;
				
				if ( photoIsLandscape ) {
					document.body.setAttribute( 'photo-orientation', 'landscape' );
				}
				else {
					document.body.setAttribute( 'photo-orientation', 'portrait' );
				}
				
				console.log( "photoIsLandscape: ", photoIsLandscape );

				var targetOrientation = photoIsLandscape ? 'landscape' : 'portrait';

				var maxWidth = document.getElementById( 'reenacter' ).clientWidth;
				var maxHeight = document.getElementById( 'reenacter' ).clientHeight;

				var currentOrientation = $( 'body' ).attr( 'orientation' );
				
				console.log( currentOrientation, " to ", targetOrientation );
				
				if ( currentOrientation != targetOrientation ) {
					var temp = maxWidth;
					maxWidth = maxHeight;
					maxHeight = temp;
				}

				//screen.orientation.lock( targetOrientation ).then( function () {
					if ( photoIsLandscape ) {
						document.getElementById( 'viewfinder' ).setAttribute( 'width', maxWidth );
						document.getElementById( 'viewfinder' ).setAttribute( 'height', maxHeight );
						
						if ( realImageWidth / realImageHeight < maxWidth / maxHeight ) {
							document.getElementById( 'original-photo' ).setAttribute( 'height', '100%' );
							document.getElementById( 'original-photo' ).removeAttribute( 'width' );
						}
						else {
							document.getElementById( 'original-photo' ).setAttribute( 'width', '100%' );
							document.getElementById( 'original-photo' ).removeAttribute( 'height' );
						}
					}
					else {
						// The swapped width/height values are intentional, since it is rotated.
						document.getElementById( 'viewfinder' ).setAttribute( 'width', maxHeight );
						document.getElementById( 'viewfinder' ).setAttribute( 'height', maxWidth );
						
						if ( realImageWidth / realImageHeight < maxWidth / maxHeight ) {
							document.getElementById( 'original-photo' ).setAttribute( 'height', '100%' );
							document.getElementById( 'original-photo' ).removeAttribute( 'width' );
						}
						else {
							document.getElementById( 'original-photo' ).setAttribute( 'width', '100%' );
							document.getElementById( 'original-photo' ).removeAttribute( 'height' );
						}
					}

					originalPhoto.style.visibility = '';
					Camera.alignViewfinder();
					
					if ( currentOrientation != targetOrientation ) {
						// Hack: we can't tell when a rotation has finished happening (and all of the screen
						// has finished redrawing, so we need to check that the viewfinder is properly
						// aligned for a little bit.
						Views.viewSpecificTimers['alignViewfinder'] = setInterval( Camera.alignViewfinder, 1000 );
					}
					console.log("getting camera");

					Camera.getCamera().then(
						function resolved() {
							Camera.startup();

							App.loaded();
						},
						function rejected( reason ) {
							alert( reason );
							Views.show( 'intro' );
						}
					);
					/*
				}, function failedToLock() {
					navigator.mozL10n.formatValue( "orientation-error" ).then( (string) => {
						alert(string);
						Views.show( 'capture' );
					} );
				} );
				*/
			};

			originalPhoto.setAttribute( 'src', photoDataURL );
		},

		'confirm' : function () {
			console.log( "In confirm" );
			
			App.loading();
			
			generateReenactedImage().then( function ( url ) {
				console.log( url );
				$( '#photo-final-confirm' ).attr( 'src', url );
				
				App.loaded();
			}, function () {
				alert( "Error" );
			} );
		},
		
		'next-step' : function () {
			App.loading();
			
			generateReenactedImage().then( function ( url ) {
				console.log( url );
				$( '#photo-final' ).attr( 'src', url );
				
				App.loaded();
			}, function () {
				alert( "Error" );
			} );
		},
	},
};

var Camera = {
	cameraObj : null,

	get cameraIndex() {
		var cameraIndex = App.cache( 'cameraIndex' );
		
		if ( ! cameraIndex ) {
			return 0;
		}
		else {
			return cameraIndex;
		}
	},

	cameraCount : 0,

	get viewfinder() {
		return document.getElementById( 'viewfinder' );
	},

	startup : function ( camera ) {
		console.log( "Camera.startup()" );
	},

	shutdown : function () {
		console.log( "Camera.shutdown()" );
	},

	capture : function () {
		console.log( "Camera.capture()" );

		// Simulate a shutter closing.
		new Audio( 'audio/shutter.opus' ).play();

		var video = document.getElementById( 'viewfinder' );
		var canvas = document.createElement( 'canvas' );
		canvas.width = video.videoWidth;
		canvas.height = video.videoHeight;
		
		var context = canvas.getContext( '2d' );
		var video = document.getElementById( 'viewfinder' );
		context.drawImage( video, 0, 0, canvas.width, canvas.height );

		document.getElementById( 'viewfinder' ).removeAttribute( 'class' );

		var imageData = canvas.toBlob( function ( imageData ) {
			App.persistentVar( 'last-photo', imageData );
		
			Views.show( 'confirm' );
		} );
	},

	alignViewfinder : function () {
		console.log( "Camera.alignViewfinder()" );
	},

	getCamera : function () {
		return new Promise( function ( resolve, reject ) {
			if ( navigator.mediaDevices && navigator.mediaDevices.getUserMedia ) {
				var video = document.getElementById( 'viewfinder' );
				
				navigator.mediaDevices.getUserMedia( { video: true } ).then( function ( stream ) {
					video.src = window.URL.createObjectURL( stream );
					video.play();
					resolve();
				} );
				
				$( 'body' ).attr( 'cameraCount', '1' );
				
				/*
				if ( availableCameras.length > 1 ) {
					document.getElementById( 'camera-switch' ).style.display = '';
				}
				else {
					document.getElementById( 'camera-switch' ).style.display = 'none';
				}

				Camera.cameraCount = availableCameras.length;
				*/
			}
			else {
				reject( "No camera available." );
			}
		} );
	}
};

window.addEventListener( 'DOMContentLoaded', function () {
	console.log( "event: window.DOMContentLoaded" );

	document.getElementById( 'restart-button' ).addEventListener( 'click', function () {
		console.log( "event: restart-button.click" );

		Views.show( 'intro' );
	} );

	document.getElementById( 'back-button' ).addEventListener( 'click', function () {
		console.log( "event: back-button.click" );

		Views.show( 'intro' );
	} );

	document.getElementById( 'confirm-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: confirm-button.click" );
		
		App.loading();

		// Save the normal photo by itself.
		App.persistentVar( 'final-photo-blob', App.persistentVar( 'last-photo' ) );
		var filename = "reenact-" + Date.now() + ".jpg";

			// Save merged image.
	
			// Find the smaller image.
			var oldImageDataURL = App.persistentVar( 'original-photo-data-url' );
	
			console.log( oldImageDataURL );
	
			var newImageDataURL = App.persistentVar( 'last-photo-data-url' );
	
			console.log( newImageDataURL );
	
			var oldImageEl = document.createElement( 'img' );
	
			oldImageEl.onload = function () {
				console.log( "oldImageEl loaded" );
		
				var newImageEl = document.createElement( 'img' );
		
				var oldImageWidth = oldImageEl.naturalWidth;
				var oldImageHeight = oldImageEl.naturalHeight;
		
				newImageEl.onload = function () {
					console.log( "newImageEl loaded" );
		
					var newImageWidth = newImageEl.naturalWidth;
					var newImageHeight = newImageEl.naturalHeight;
			
					console.log( "width / height", newImageWidth, newImageHeight );
			
					var canvas = document.createElement( 'canvas' );
					var context = canvas.getContext( '2d' );
			
					if ( newImageWidth < newImageHeight ) {
						// Portrait.
						console.log( "Portrait orientation." );
				
						var smallestHeight = Math.min( oldImageHeight, newImageHeight );
						var totalWidth = ( ( smallestHeight / oldImageHeight ) * oldImageWidth ) + ( ( smallestHeight / newImageHeight ) * newImageWidth );
						var totalHeight = smallestHeight;
				
						canvas.height = totalHeight;
						canvas.width = totalWidth;
				
						console.log( "canvas height/width", canvas.height, canvas.width );
				
						context.drawImage( oldImageEl, 0, 0, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), ( ( smallestHeight / oldImageHeight ) * oldImageHeight ) );
						context.drawImage( newImageEl, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), 0, ( ( smallestHeight / newImageHeight ) * newImageWidth ), ( ( smallestHeight / newImageHeight ) * newImageHeight ) );
					}
					else {
						// Landscape
						console.log( "Landscape orientation." );

						var smallestWidth = Math.min( oldImageWidth, newImageWidth );
						var totalHeight = ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) + ( ( smallestWidth / newImageWidth ) * newImageHeight );
						var totalWidth = smallestWidth;
				
						canvas.height = totalHeight;
						canvas.width = totalWidth;
				
						console.log( "canvas height/width", canvas.height, canvas.width );
				
						context.drawImage( oldImageEl, 0, 0, ( ( smallestWidth / oldImageWidth ) * oldImageWidth ), ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) );
						context.drawImage( newImageEl, 0, ( ( smallestWidth / oldImageWidth ) * oldImageHeight ), ( ( smallestWidth / newImageWidth ) * newImageWidth ), ( ( smallestWidth / newImageWidth ) * newImageHeight ) );
					}
				
					console.log( "Finished drawing." );
			
					canvas.toBlob( function ( blob ) {
						console.log( "Blob received: ", blob );
				
						App.persistentVar( 'final-photo-blob', blob );
				
						var url = window.URL.createObjectURL(blob);
						console.log(url);
						var downloadLink = $( '<a/>' ).attr( 'download', filename ).attr( 'href', url );;
						console.log(downloadLink);
						console.log(filename);
						downloadLink.click();
//						window.URL.revokeObjectURL(url);

						Views.show( 'next-step' );
					}, "image/jpeg" );
				};
		
				newImageEl.setAttribute( 'src', newImageDataURL );
			};
	
			oldImageEl.setAttribute( 'src', oldImageDataURL );

	} );
	
	document.getElementById( 'cancel-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: cancel-button.click" );

		Views.show( 'capture' );
	} );

	document.getElementById( 'share-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: share-button.click" );

		document.location.href = App.persistentVar( 'final-photo-url' );
	} );

	
	document.getElementById( 'restart-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: restart-button.click" );
		
		Views.show( 'intro' );
	} );

	document.getElementById( 'camera-switch' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: camera-switch.click" );

		var cameraIndex = parseInt( App.cache( 'cameraIndex' ), 10 ) || 0;
		cameraIndex++;
		cameraIndex %= Camera.cameraCount;
		App.cache( 'cameraIndex', cameraIndex );

		Views.show( 'capture' );
	} );

	App.startup();
} );

screen.onmozorientationchange = function ( arg ) {
	console.log( "event: screen.onmozorientationchange" );

	App.setOrientation();
};

window.addEventListener( 'beforeunload', function() {
	console.log( "event: window.beforeunload" );

	Camera.shutdown();
} );
/*

document.addEventListener( 'visibilitychange', function ( evt ) {
	console.log( "event: document.visibilitychange", document.hidden );

	if ( document.hidden ) {
		// Release the camera so that other apps can use it.
		Camera.shutdown();
	}
	else {
		App.setOrientation();
		
		// Restore the previously active view.
		var currentScreen = App.persistentVar( 'current-screen' );
		
		console.log( "currentScreen: ", currentScreen );
		
		if ( currentScreen ) {
			if ( currentScreen != 'intro' ) {
				Views.show( currentScreen );
			}
		}
		else {
			Views.show( 'intro' );
		}
	}
} );
*/
jQuery( function ( $ ) {
	$( '#choose-photo' ).on( 'change', function ( e ) {
		var file = e.target.files[0];
	
		console.log( file );
	
		App.loading();

		var reader = new FileReader();
		reader.readAsDataURL( file );
		reader.onloadend = function() {
			App.persistentVar( 'original-photo-data-url', reader.result );

			Views.show( 'capture' );
		};
	} );
	
	$( '#shutter-release' ).on( 'click', function ( evt ) {
		console.log( "event: shutter-release.click" );

		Camera.capture();
	} );
	
	if ( $( window ).width() > $( window ).height() ) {
		$( 'body' ).attr( 'orientation', 'landscape' );
	}
	else {
		$( 'body' ).attr( 'orientation', 'portrait' );
	}
} );

function generateReenactedImage() {
	return new Promise( function ( resolve, reject ) {
		// Find the smaller image.
		var oldImageDataURL = App.persistentVar( 'original-photo-data-url' );
		
		var reader = new FileReader();
		reader.readAsDataURL( App.persistentVar( 'last-photo' ) );
		reader.onloadend = function() {
			App.persistentVar( 'last-photo-data-url', reader.result );

			var newImageDataURL = App.persistentVar( 'last-photo-data-url' );
			
			console.log( oldImageDataURL );
			console.log( newImageDataURL );

			var oldImageEl = document.createElement( 'img' );

			oldImageEl.onload = function () {
				console.log( "oldImageEl loaded" );

				var newImageEl = document.createElement( 'img' );

				var oldImageWidth = oldImageEl.naturalWidth;
				var oldImageHeight = oldImageEl.naturalHeight;

				newImageEl.onload = function () {
					console.log( "newImageEl loaded" );

					var newImageWidth = newImageEl.naturalWidth;
					var newImageHeight = newImageEl.naturalHeight;
	
					console.log( "width / height", newImageWidth, newImageHeight );
	
					var canvas = document.createElement( 'canvas' );
					var context = canvas.getContext( '2d' );
	
					if ( newImageWidth < newImageHeight ) {
						// Portrait.
						console.log( "Portrait orientation." );
		
						var smallestHeight = Math.min( oldImageHeight, newImageHeight );
						var totalWidth = ( ( smallestHeight / oldImageHeight ) * oldImageWidth ) + ( ( smallestHeight / newImageHeight ) * newImageWidth );
						var totalHeight = smallestHeight;
		
						canvas.height = totalHeight;
						canvas.width = totalWidth;
		
						console.log( "canvas height/width", canvas.height, canvas.width );
		
						context.drawImage( oldImageEl, 0, 0, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), ( ( smallestHeight / oldImageHeight ) * oldImageHeight ) );
						context.drawImage( newImageEl, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), 0, ( ( smallestHeight / newImageHeight ) * newImageWidth ), ( ( smallestHeight / newImageHeight ) * newImageHeight ) );
					}
					else {
						// Landscape
						console.log( "Landscape orientation." );

						var smallestWidth = Math.min( oldImageWidth, newImageWidth );
						var totalHeight = ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) + ( ( smallestWidth / newImageWidth ) * newImageHeight );
						var totalWidth = smallestWidth;
		
						canvas.height = totalHeight;
						canvas.width = totalWidth;
		
						console.log( "canvas height/width", canvas.height, canvas.width );
		
						context.drawImage( oldImageEl, 0, 0, ( ( smallestWidth / oldImageWidth ) * oldImageWidth ), ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) );
						context.drawImage( newImageEl, 0, ( ( smallestWidth / oldImageWidth ) * oldImageHeight ), ( ( smallestWidth / newImageWidth ) * newImageWidth ), ( ( smallestWidth / newImageWidth ) * newImageHeight ) );
					}
		
					console.log( "Finished drawing." );
	
					canvas.toBlob( function ( blob ) {
						console.log( "Blob received: ", blob );
		
						App.persistentVar( 'final-photo-blob', blob );
						var url = window.URL.createObjectURL(blob);
						App.persistentVar( 'final-photo-url', url );
				
						resolve( url );
					}, "image/jpeg" );
				};

				newImageEl.setAttribute( 'src', newImageDataURL );
			};

			oldImageEl.setAttribute( 'src', oldImageDataURL );
		};
	} );
}