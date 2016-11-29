"use strict";

var App = {
	persistentVars : { },

	videoStream : null,

	// A guess as to whether the shared camera is front-facing and we should flip it.
	cameraIsFrontFacing : false,

	checkSupport : function () {
		return navigator.mediaDevices && navigator.mediaDevices.getUserMedia && window.URL && window.URL.createObjectURL;
	},

	startup : function () {
		App.setOrientation();

		if ( App.checkSupport() ) {
			$( 'body' ).removeClass( 'unsupported' );
			
			if ( navigator.mediaDevices.enumerateDevices ) {
				navigator.mediaDevices.enumerateDevices().then( function ( devices ) {
					var cameraCount = 0;
		
					devices.forEach( function ( device ) {
						if ( 'videoinput' === device.kind ) {
							cameraCount++;
						}
					} );
					
					if ( cameraCount === 0 ) {
						$( 'body' ).addClass( 'no-camera' );
					}
					else if ( cameraCount === 1 ){
						// I think every device with a rear camera also has a selfie camera.
						App.cameraIsFrontFacing = true;
					}
					
					Views.show( 'intro' );
				} );
			}
			else {
				Views.show( 'intro' );
				
				if ( ! ( 'ontouchstart' in window ) ) {
					// Assume any non-touchscreen device is a desktop browser that will default to selfie camera.
					App.cameraIsFrontFacing = true;
				}
			}
		}
		else {
			$( 'body' ).addClass( 'unsupported' );
			
			Views.show( 'intro' );
		}
	},

	showScreen : function ( screenId ) {
		var screens = document.getElementsByClassName( 'app-screen' );

		for ( var i = 0; i < screens.length; i++ ) {
			screens.item( i ).style.display = 'none';
		}

		document.getElementById( 'screen-' + screenId ).style.display = 'block';

		App.persistentVar( 'current-screen', screenId );
	},

	setOrientation : function () {
		if ( $( window ).width() < $( window ).height() ) {
			document.body.setAttribute( 'orientation', 'portrait' );
		}
		else {
			document.body.setAttribute( 'orientation', 'landscape' );
		}
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
		App.setOrientation();
		
		Views.show( App.persistentVar( 'current-screen' ) );
	},
	
	getCamera : function () {
		return new Promise( function ( resolve, reject ) {
			var video = document.getElementById( 'viewfinder' );
			
			navigator.mediaDevices.getUserMedia( { video: true } ).then( function ( stream ) {
				console.log( stream );
				App.videoStream = stream;
				
				video.src = window.URL.createObjectURL( stream );
				
				video.addEventListener( "playing", function () {
					resolve();
				}, true );
				
				video.play();
			} );
		} );
	},
	
	capture : function () {
		return new Promise( function ( resolve ) {
			// Simulate a shutter closing.
			new Audio( 'audio/shutter.opus' ).play();
	
			document.getElementById( 'reenacter' ).style.visibility = 'hidden';
		
			App.loading();
		
			var video = document.getElementById( 'viewfinder' );
			var canvas = document.createElement( 'canvas' );
			canvas.width = video.videoWidth;
			canvas.height = video.videoHeight;
		
			var context = canvas.getContext( '2d' );
			var video = document.getElementById( 'viewfinder' );
			context.drawImage( video, 0, 0, canvas.width, canvas.height );

			document.getElementById( 'viewfinder' ).removeAttribute( 'class' );

			canvas.toBlob( function ( imageData ) {
				App.persistentVar( 'last-photo', imageData );
		
				resolve();
			} );
		} );
	}
};

var Views = {
	show : function ( screenId ) {
		App.loaded();
		
		if ( App.videoStream ) {
			if ( App.videoStream.getVideoTracks ) {
				App.videoStream.getVideoTracks().forEach( function ( track ) {
					track.stop();
				} );
			}
			else {
				App.videoStream.stop();
			}
			
			App.videoStream = null;
		}
		
		if ( screenId in Views.preViewHandlers ) {
			Views.preViewHandlers[screenId]();
		}

		App.showScreen( screenId );
		
		if ( screenId in Views.viewHandlers ) {
			Views.viewHandlers[screenId]();
		}
	},

	preViewHandlers : {
		'capture' : function () {
			document.getElementById( 'reenacter' ).style.visibility = '';
			
			document.getElementById( 'original-photo' ).style.visibility = 'hidden';
			document.getElementById( 'shutter-release' ).removeAttribute( 'disabled' );
			
			document.getElementById( 'viewfinder' ).setAttribute( 'class', 'fading' );
			
			$( 'body' ).removeClass( 'front-facing-camera' );
			
			if ( App.cameraIsFrontFacing ) {
				$( '#camera-mirror' ).click();
			}
		},
		
		'next-step' : function () {
			$( '#download-button' ).attr( 'download', 'reenact-' + Date.now() + '.jpg' );
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

				originalPhoto.style.visibility = '';

				App.getCamera().then(
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
			App.loading();
			
			generateReenactedImage().then( function ( url ) {
				$( '#photo-final-confirm' ).attr( 'src', url );
				
				App.loaded();
			}, function () {
				alert( "Error" );
			} );
		},
		
		'next-step' : function () {
			App.loading();
			
			// This is generated by the confirm screen.
			var url = App.persistentVar( 'final-photo-url' );

			$( '#photo-final' ).attr( 'src', url );
			
			$( '#download-button' ).attr( 'href', url );
				
			App.loaded();
		},
	}
};

jQuery( function ( $ ) {
	var resizeTimeout = null;

	$( window ).on( 'resize', function () {
		clearTimeout( resizeTimeout );
	
		resizeTimeout = setTimeout( App.handleResize, 250 );
	} );

	$( '#choose-photo' ).on( 'change', function ( e ) {
		var file = e.target.files[0];
	
		App.loading();

		var reader = new FileReader();
		reader.readAsDataURL( file );
		reader.onloadend = function() {
			App.persistentVar( 'original-photo-data-url', reader.result );

			Views.show( 'capture' );
		};
	} );
	
	$( '#shutter-release' ).on( 'click', function ( evt ) {
		$( this ).attr( 'disabled', 'disabled' );
		
		App.capture().then( function () {
			Views.show( 'confirm' );
		} );
	} );
	
	$( '#restart-button, #back-button' ).on( 'click', function ( e ) {
		e.preventDefault();

		Views.show( 'intro' );
	} );

	$( '#confirm-button' ).on( 'click', function ( e ) {
		e.preventDefault();
		
		App.loading();

		Views.show( 'next-step' );
	} );
	
	$( '#cancel-button' ).on( 'click', function ( e ) {
		e.preventDefault();

		Views.show( 'capture' );
	} );

	$( '#share-button' ).on( 'click', function ( e ) {
		e.preventDefault();

		document.location.href = App.persistentVar( 'final-photo-url' );
	} );
	
	$( '#camera-mirror' ).on( 'click', function ( e ) {
		e.preventDefault();
		
		if ( $( "body" ).hasClass( 'front-facing-camera' ) ) {
			$( "body" ).removeClass( 'front-facing-camera' );
		}
		else {
			$( "body" ).addClass( "front-facing-camera" );
		}
	} );
	
	$( '#help-button' ).on( 'click', function ( e ) {
		e.preventDefault();
		
		Views.show( 'help' );
	} );


	$( '#help-cancel-button' ).on( 'click', function ( e ) {
		e.preventDefault();
		
		Views.show( 'intro' );
	} );

	App.startup();
	
	$( document ).on( 'keydown', function ( e ) {
		if ( e.keyCode === 27 || e.keyCode === 8 || e.keyCode === 46 ) {
			// Escape, backspace, and delete. Same as clicking the secondary button.
			var buttons = $( '.buttons .secondary:visible' );

			if ( buttons ) {
				// Don't override if there is no secondary button, like on the intro page.
				e.preventDefault();
				buttons.first().click();
			}
		}
		else if ( e.keyCode === 13 || e.keyCode === 32 ) {
			e.preventDefault();
			
			// Enter and space bar. Same as clicking the primary button or the "Choose photo" button.
			var buttons = $( '.buttons .primary:visible' );
			
			if ( buttons ) {
				e.preventDefault();

				buttons.first().click();
			}
		}
	} );
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
			
			var oldImageEl = document.createElement( 'img' );

			oldImageEl.onload = function () {
				var newImageEl = document.createElement( 'img' );

				var oldImageWidth = oldImageEl.naturalWidth;
				var oldImageHeight = oldImageEl.naturalHeight;

				newImageEl.onload = function () {
					var newImageWidth = newImageEl.naturalWidth;
					var newImageHeight = newImageEl.naturalHeight;
	
					var canvas = document.createElement( 'canvas' );
					var context = canvas.getContext( '2d' );
	
					// Portrait.
					var smallestHeight = Math.min( oldImageHeight, newImageHeight );
					var totalWidth = ( ( smallestHeight / oldImageHeight ) * oldImageWidth ) + ( ( smallestHeight / newImageHeight ) * newImageWidth );
					var totalHeight = smallestHeight;
	
					canvas.height = totalHeight;
					canvas.width = totalWidth;
	
					context.drawImage( oldImageEl, 0, 0, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), ( ( smallestHeight / oldImageHeight ) * oldImageHeight ) );
					context.drawImage( newImageEl, ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), 0, ( ( smallestHeight / newImageHeight ) * newImageWidth ), ( ( smallestHeight / newImageHeight ) * newImageHeight ) );
		
					canvas.toBlob( function ( blob ) {
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