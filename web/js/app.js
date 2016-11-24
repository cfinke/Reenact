"use strict";

var App = {
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

		if ( $( window ).width() < $( window ).height() ) {
			document.body.setAttribute( 'orientation', 'portrait' );
		}
		else {
			document.body.setAttribute( 'orientation', 'landscape' );
		}

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
	
	handleResize : function () {
		console.log( "Resize." );
		App.setOrientation();
		Views.show( App.persistentVar( 'current-screen' ) );
	}
};

var Views = {
	show : function ( screenId ) {
		App.loaded();
		
		if ( screenId in Views.preViewHandlers ) {
			Views.preViewHandlers[screenId]();
		}

		App.showScreen( screenId );
		
		if ( screenId in Views.viewHandlers ) {
			Views.viewHandlers[screenId]();
		}
		else {
			console.log( "No view handler for " + screenId );
		}
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

			var maxWidth = document.getElementById( 'reenacter' ).clientWidth;
			var maxHeight = document.getElementById( 'reenacter' ).clientHeight;

			originalPhoto.onload = function () {
				originalPhoto.onload = null;

				var realImageWidth = originalPhoto.naturalWidth;
				var realImageHeight = originalPhoto.naturalHeight;

				// Center the image, making it as big as possible.
				if ( realImageWidth / realImageHeight < maxWidth / maxHeight ) {
					originalPhoto.style.height = '100%';
					originalPhoto.style.width = 'auto';
				}
				else {
					originalPhoto.style.width = '100%';
					originalPhoto.style.height = 'auto';
				}

				var photoIsLandscape = realImageWidth > realImageHeight;
				
				if ( photoIsLandscape ) {
					document.body.setAttribute( 'photo-orientation', 'landscape' );
				}
				else {
					document.body.setAttribute( 'photo-orientation', 'portrait' );
				}
				
				console.log( "photoIsLandscape: ", photoIsLandscape );

				originalPhoto.style.visibility = '';
				
				Camera.alignViewfinder();
				
				console.log("getting camera");

				Camera.getCamera().then(
					function resolved() {
						var video = document.getElementById( 'viewfinder' );
						
						if ( video.videoWidth / video.videoHeight < maxWidth / maxHeight ) {
							video.style.height = '100%';
							video.style.width = 'auto';
							video.style.left = Math.floor( ( $( '#reenacter' ).width() - $( '#viewfinder' ).width() ) / 2 ) + "px";
							video.style.top = '0';
						}
						else {
							video.style.width = '100%';
							video.style.height = 'auto';
							video.style.top = Math.floor( ( $( '#reenacter' ).height() - $( '#viewfinder' ).height() ) / 2 ) + "px";
							video.style.left = '0';
						}
						
						App.loaded();
					},
					function rejected( reason ) {
						alert( reason );
						Views.show( 'intro' );
					}
				);
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
					
					video.addEventListener( "playing", function () {
						resolve();
					}, true );
					
					video.play();
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

window.addEventListener( "deviceorientation", function () {
	console.log( "event: screen.onmozorientationchange" );

	App.handleResize();
}, true );

var resizeTimeout = null;

$( window ).on( 'resize', function () {
	clearTimeout( resizeTimeout );
	
	resizeTimeout = setTimeout( App.handleResize, 250 );
} );

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