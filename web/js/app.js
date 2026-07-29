"use strict";

var App = {
	persistentVars : { },

	videoStream : null,

	selectedCameraIndex : null,
	availableCameras : [],

	// A guess as to whether the shared camera is front-facing and we should flip it.
	cameraIsFrontFacing : false,

	currentImageSettings : {
		width : 0,
		height : 0,
	},

	currentStreamSettings : {
		width : 0,
		height : 0,
	},

	checkSupport : function () {
		return navigator.mediaDevices && navigator.mediaDevices.enumerateDevices && navigator.mediaDevices.getUserMedia && window.URL && window.URL.createObjectURL;
	},

	startup : function () {
		App.setOrientation();

		document.body.classList.add( 'unsupported' );

		if ( App.checkSupport() ) {
			document.body.classList.remove( 'unsupported' );
		}

		Views.show( 'intro' );
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
		if ( window.innerWidth < window.innerHeight ) {
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
		document.body.classList.add( 'loading' );
		document.body.classList.remove( 'loaded' );
	},

	loaded : function () {
		document.body.classList.add( 'loaded' );
		document.body.classList.remove( 'loading' );
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
			// This call is just to get the permissions prompt, without which iOS won't show all cameras when calling enumerateDevices.
			navigator.mediaDevices.getUserMedia(
				{
					audio: false,
					video: {
						width: { ideal: App.currentImageSettings.width },
						height: { ideal: App.currentImageSettings.height }
					}
				}
			).then( function ( stream ) {
				App.currentStreamSettings = stream.getVideoTracks()[0].getSettings();

				// Then we can enumerate the cameras to find out if there are multiple cameras, causing us to show the "switch camera" icon.
				return navigator.mediaDevices.enumerateDevices().then( function ( devices ) {
					// This stream was only needed to trigger the permissions prompt; stop it so that
					// the camera is free when we request the specific device we want below.
					stream.getVideoTracks().forEach( function ( track ) {
						track.stop();
					} );

					var video = document.getElementById( 'viewfinder' );

					App.availableCameras = [];
					App.cameraIsFrontFacing = false;

					devices.forEach( function ( device ) {
						if ( 'videoinput' === device.kind ) {
							App.availableCameras.push( device );
						}
					} );

					if ( App.availableCameras.length === 0 ) {
						throw 'No camera available.';
					}
					else {
						if ( App.availableCameras.length === 1 ){
							// I think every device with a rear camera also has a selfie camera, so if there's only one, it's probably forward-facing.
							App.cameraIsFrontFacing = true;

							document.getElementById( 'camera-switch' ).style.display = 'none';
						}
						else if ( ! ( 'ontouchstart' in window ) ) {
							// Assume any non-touchscreen device is a desktop browser that will default to selfie camera.
							App.cameraIsFrontFacing = true;
						}

						if ( App.selectedCameraIndex === null || App.selectedCameraIndex >= App.availableCameras.length ) {
							// Either this is the first camera request or the previously selected
							// camera no longer exists; fall back to the first available camera.
							App.selectedCameraIndex = 0;
						}

						var video = document.getElementById( 'viewfinder' );

						// Then we can call getUserMedia on the right camera, so we can switch between cameras.
						return navigator.mediaDevices.getUserMedia(
							{
								audio: false,
								video: {
									deviceId : { exact: App.availableCameras[ App.selectedCameraIndex ].deviceId },
									width: { ideal: App.currentImageSettings.width },
									height: { ideal: App.currentImageSettings.height }
								}
							} ).then( function ( stream ) {
							App.currentStreamSettings = stream.getVideoTracks()[0].getSettings();

							// If the browser reports which way this camera faces, prefer that
							// over the guesses made above.
							if ( App.currentStreamSettings.facingMode ) {
								App.cameraIsFrontFacing = ( 'user' === App.currentStreamSettings.facingMode );
							}

							App.videoStream = stream;

							video.srcObject = stream;

							video.addEventListener( "playing", function () {
								resolve();
							}, true );

							video.play();
						} );
					}
				} );
			} ).catch( function ( e ) {
				document.body.classList.add( 'no-camera' );
				Views.show( 'intro' );

				// Alternatively:
				// reject( 'Reenact must have access to the camera to function.' );
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

			// The video's intrinsic dimensions reflect the frames as they're actually being
			// rendered (including any rotation the browser applied for device orientation),
			// so capturing at exactly this size can never stretch or squash the image.
			canvas.width = video.videoWidth;
			canvas.height = video.videoHeight;

			var context = canvas.getContext( '2d' );

			// When the viewfinder is mirrored via CSS, mirror the capture too so that the
			// saved photo matches what the user aligned on screen.
			if ( document.body.classList.contains( 'front-facing-camera' ) ) {
				context.translate( canvas.width, 0 );
				context.scale( -1, 1 );
			}

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
	finalPhotoAsFile : function () {
		return new File( [ App.persistentVar( 'final-photo-blob' ) ], 'reenact-' + Date.now() + '.jpg', { type: 'image/jpeg' } );
	},

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
			document.querySelectorAll( '[data-relies-on-camera]' ).forEach( function ( el ) {
				el.setAttribute( 'disabled', 'disabled' );
			} );

			document.getElementById( 'reenacter' ).style.visibility = '';

			document.getElementById( 'original-photo' ).style.visibility = 'hidden';

			document.getElementById( 'viewfinder' ).setAttribute( 'class', 'fading' );

			document.body.classList.remove( 'front-facing-camera' );
		},

		'next-step' : function () {
			document.getElementById( 'download-button' ).setAttribute( 'download', 'reenact-' + Date.now() + '.jpg' );

			// Only show the share button if the browser can share the photo; the download
			// button is always available as an alternative.
			var canSharePhoto = false;

			if ( navigator.canShare && navigator.share ) {
				canSharePhoto = navigator.canShare( { files: [ Views.finalPhotoAsFile() ] } );
			}

			document.getElementById( 'share-button' ).style.display = canSharePhoto ? '' : 'none';
		}
	},

	viewHandlers : {
		'intro' : function () {
			App.persistentVar( 'original-photo', null );

			// The reenacted photo can't be reached from here, so release its URL.
			if ( App.persistentVar( 'final-photo-url' ) ) {
				window.URL.revokeObjectURL( App.persistentVar( 'final-photo-url' ) );
				App.persistentVar( 'final-photo-url', null );
			}

			// Clear the file input so that choosing the same photo again fires another change event.
			document.getElementById( 'choose-photo' ).value = '';
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

				App.currentImageSettings.width = realImageWidth;
				App.currentImageSettings.height = realImageHeight;

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
						document.querySelectorAll( '[data-relies-on-camera]' ).forEach( function ( el ) {
							el.removeAttribute( 'disabled' );
						} );

						// This can't be decided until now, since it isn't known which camera
						// will be used (or which way it faces) until getCamera() finishes.
						if ( App.cameraIsFrontFacing ) {
							document.body.classList.add( 'front-facing-camera' );
						}

						var video = document.getElementById( 'viewfinder' );
						var reenacter = document.getElementById( 'reenacter' );

						if ( video.videoWidth / video.videoHeight < maxWidth / maxHeight ) {
							video.style.height = '100%';
							video.style.width = 'auto';
							video.style.left = Math.floor( ( reenacter.offsetWidth - video.offsetWidth ) / 2 ) + "px";
							video.style.top = '0';
						}
						else {
							video.style.width = '100%';
							video.style.height = 'auto';
							video.style.top = Math.floor( ( reenacter.offsetHeight - video.offsetHeight ) / 2 ) + "px";
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
				document.getElementById( 'photo-final-confirm' ).setAttribute( 'src', url );

				App.loaded();
			}, function () {
				alert( "Error" );
			} );
		},

		'next-step' : function () {
			App.loading();

			// This is generated by the confirm screen.
			var url = App.persistentVar( 'final-photo-url' );

			document.getElementById( 'photo-final' ).setAttribute( 'src', url );

			document.getElementById( 'download-button' ).setAttribute( 'href', url );

			App.loaded();
		},
	}
};

document.addEventListener( 'DOMContentLoaded', function () {
	var resizeTimeout = null;

	window.addEventListener( 'resize', function () {
		clearTimeout( resizeTimeout );

		resizeTimeout = setTimeout( App.handleResize, 250 );
	} );

	document.getElementById( 'choose-photo' ).addEventListener( 'change', function ( e ) {
		var file = e.target.files[0];

		if ( ! file ) {
			return;
		}

		App.loading();

		var reader = new FileReader();
		reader.readAsDataURL( file );
		reader.onloadend = function() {
			App.persistentVar( 'original-photo-data-url', reader.result );

			Views.show( 'capture' );
		};
	} );

	document.getElementById( 'shutter-release' ).addEventListener( 'click', function ( evt ) {
		this.setAttribute( 'disabled', 'disabled' );

		App.capture().then( function () {
			Views.show( 'confirm' );
		} );
	} );

	document.querySelectorAll( '#restart-button, #back-button' ).forEach( function ( el ) {
		el.addEventListener( 'click', function ( e ) {
			e.preventDefault();

			Views.show( 'intro' );
		} );
	} );

	document.getElementById( 'confirm-button' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		App.loading();

		Views.show( 'next-step' );
	} );

	document.getElementById( 'cancel-button' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		Views.show( 'capture' );
	} );

	document.getElementById( 'share-button' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		navigator.share( { files: [ Views.finalPhotoAsFile() ] } ).catch( function () {
			// The user backing out of the share sheet rejects the promise; there's nothing to handle.
		} );
	} );

	document.getElementById( 'camera-mirror' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		document.body.classList.toggle( 'front-facing-camera' );
	} );

	document.getElementById( 'camera-switch' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		var nextCameraIndex;

		if ( null === App.selectedCameraIndex ) {
			nextCameraIndex = 0;
		}
		else {
			nextCameraIndex = ( App.selectedCameraIndex + 1 ) % App.availableCameras.length;
		}

		App.selectedCameraIndex = nextCameraIndex;

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

		Views.show( 'capture' );
	} );

	document.getElementById( 'help-button' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		Views.show( 'help' );
	} );


	document.getElementById( 'help-cancel-button' ).addEventListener( 'click', function ( e ) {
		e.preventDefault();

		Views.show( 'intro' );
	} );

	App.startup();

	// A replacement for jQuery's :visible - an element is visible if it or an
	// ancestor isn't hidden via display: none.
	function visibleElements( selector ) {
		return Array.prototype.filter.call( document.querySelectorAll( selector ), function ( el ) {
			return null !== el.offsetParent;
		} );
	}

	document.addEventListener( 'keydown', function ( e ) {
		var buttons;

		if ( 'Escape' === e.key || 'Backspace' === e.key || 'Delete' === e.key ) {
			// Same as clicking the secondary button.
			buttons = visibleElements( '.buttons .secondary' );

			if ( buttons.length ) {
				// Don't override if there is no secondary button, like on the intro page.
				e.preventDefault();
				buttons[0].click();
			}
		}
		else if ( 'Enter' === e.key || ' ' === e.key ) {
			// Same as clicking the primary button or the "Choose photo" button.
			buttons = visibleElements( '.buttons .primary' );

			if ( buttons.length ) {
				e.preventDefault();

				buttons[0].click();
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

						// Release the previous composite's URL (if any) so its memory can be reclaimed.
						if ( App.persistentVar( 'final-photo-url' ) ) {
							window.URL.revokeObjectURL( App.persistentVar( 'final-photo-url' ) );
						}

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
