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
	lockedOrientationViews : [ 'capture', 'confirm', 'next-step' ],

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

		if ( Views.lockedOrientationViews.indexOf( screenId ) == -1 || ! App.persistentVar( 'lockedOrientation' ) ) {
			screen.mozUnlockOrientation();
			continueView();
		}
		else {
			screen.orientation.lock( App.persistentVar( 'lockedOrientation' ) ).then( continueView );
		}
	},

	preViewHandlers : {
		'capture' : function () {
			document.getElementById( 'original-photo' ).style.visibility = 'hidden';
			
			document.getElementById( 'viewfinder' ).setAttribute( 'class', 'fading' );
		},
		
		'confirm' : function () {
			document.getElementById( 'photo-comparison' ).style.visibility = 'hidden';
			document.getElementById( 'photo-comparison' ).style.paddingTop = '0';
		},
	},

	viewHandlers : {
		'intro' : function () {
			App.persistentVar( 'original-photo', null );
			App.persistentVar( 'lockedOrientation', null );
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

				App.persistentVar( 'lockedOrientation', targetOrientation );

				var maxWidth = document.getElementById( 'reenacter' ).clientWidth;
				var maxHeight = document.getElementById( 'reenacter' ).clientHeight;

				var currentOrientation = screen.orientation.type.split( '-' )[0];
				
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

					Camera.getCamera().then(
						function resolved( cameraControl ) {
							Camera.startup( cameraControl.camera );
							
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
			document.getElementById( 'photo-then' ).setAttribute( 'src', photoDataURL );
		},

		'confirm' : function () {
			App.loading();
			
			screen.orientation.lock( 'portrait' ).then( function () {
				var newPhoto = document.getElementById( 'photo-now' );

				var reader = new FileReader();
				reader.readAsDataURL( App.persistentVar( 'last-photo' ) );
				reader.onloadend = function() {
					App.persistentVar( 'last-photo-data-url', reader.result );
				
					newPhoto.onload = function () {
						newPhoto.onload = null;
					
						var oldPhoto = document.getElementById( 'photo-then' );
					
						var oldImageWidth = oldPhoto.naturalWidth;
						var oldImageHeight = oldPhoto.naturalHeight;
			
						var newImageWidth = newPhoto.naturalWidth;
						var newImageHeight = newPhoto.naturalHeight;
				
						if ( newImageWidth < newImageHeight ) {
							// Portrait.
							var shorterHeight = Math.min( oldImageHeight, newImageHeight );
						
							var sameHeightOldImageHeight = ( shorterHeight / oldImageHeight ) * oldImageHeight;
							var sameHeightNewImageHeight = ( shorterHeight / newImageHeight ) * newImageHeight;

							var sameHeightOldImageWidth = ( shorterHeight / oldImageHeight ) * oldImageWidth;
							var sameHeightNewImageWidth = ( shorterHeight / newImageHeight ) * newImageWidth;

							var totalSameHeightWidth = sameHeightOldImageWidth + sameHeightNewImageWidth;
						
							var resizeRatio = ( window.innerWidth * .95 ) / totalSameHeightWidth;
						
							oldPhoto.setAttribute( 'height', Math.floor( sameHeightOldImageHeight * resizeRatio ) );
							oldPhoto.setAttribute( 'width', Math.floor( sameHeightOldImageWidth * resizeRatio ) );

							newPhoto.setAttribute( 'height', Math.floor( sameHeightNewImageHeight * resizeRatio ) );
							newPhoto.setAttribute( 'width', Math.floor( sameHeightNewImageWidth * resizeRatio ) );
							
							var photosHeight = Math.floor( sameHeightOldImageHeight * resizeRatio );
						}
						else {
							// Landscape.
							var shorterWidth = Math.min( oldImageWidth, newImageWidth );
						
							var sameWidthOldImageHeight = ( shorterWidth / oldImageWidth ) * oldImageHeight;
							var sameWidthNewImageHeight = ( shorterWidth / newImageWidth ) * newImageHeight;

							var sameWidthOldImageWidth = ( shorterWidth / oldImageWidth ) * oldImageWidth;
							var sameWidthNewImageWidth = ( shorterWidth / newImageWidth ) * newImageWidth;
						
							var resizeRatio = ( window.innerWidth * .95 ) / shorterWidth;
							
							if ( resizeRatio * ( sameWidthOldImageHeight + sameWidthNewImageHeight ) > document.getElementById( 'photo-comparison' ).clientHeight ) {
								resizeRatio = document.getElementById( 'photo-comparison' ).clientHeight / ( resizeRatio * ( sameWidthOldImageHeight + sameWidthNewImageHeight ) );
							}
						
							oldPhoto.setAttribute( 'height', Math.floor( sameWidthOldImageHeight * resizeRatio ) );
							oldPhoto.setAttribute( 'width', Math.floor( sameWidthOldImageWidth * resizeRatio ) );

							newPhoto.setAttribute( 'height', Math.floor( sameWidthNewImageHeight * resizeRatio ) );
							newPhoto.setAttribute( 'width', Math.floor( sameWidthNewImageWidth * resizeRatio ) );
							
							var photosHeight = Math.floor( sameWidthNewImageHeight * resizeRatio ) + Math.floor( sameWidthOldImageHeight * resizeRatio );
						}
						
						var containerHeight = document.getElementById( 'photo-comparison' ).clientHeight;
						var topMargin = Math.max( 0, Math.round( ( containerHeight - photosHeight ) / 2 ) );

						document.getElementById( 'photo-comparison' ).style.paddingTop = topMargin + 'px';
						document.getElementById( 'photo-comparison' ).style.visibility = '';
						
						App.loaded();
					};
				
					newPhoto.setAttribute( 'src', reader.result );
				};
			} );
		},
		
		'next-step' : function () {
			App.loading();
			
			screen.orientation.lock( 'portrait' ).then( function () {
				var finalPhoto = document.getElementById( 'photo-final' );

				var reader = new FileReader();
				reader.readAsDataURL( App.persistentVar( 'final-photo-blob' ) );
				reader.onloadend = function() {
					finalPhoto.onload = function () {
						App.loaded()
					};
					
					finalPhoto.setAttribute( 'src', reader.result );
				};
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

		Camera.cameraObj = camera;
		Camera.viewfinder.mozSrcObject = Camera.cameraObj;
		Camera.viewfinder.play();
	},

	shutdown : function () {
		console.log( "Camera.shutdown()" );

		if ( Camera.cameraObj ) {
			Camera.viewfinder.mozSrcObject = null;
			Camera.cameraObj.release();
			Camera.cameraObj = null;
		}
	},

	capture : function () {
		console.log( "Camera.capture()" );

		var rotation = 0;

		switch ( screen.mozOrientation ) {
			case 'portrait-secondary':
				rotation = 180;
			break;
			case 'landscape-primary':
				rotation = 270;
			break;
			case 'landscape-secondary':
				rotation = 90;
			break;
		}

		Camera.cameraObj.takePicture(
			{
				dateTime : Math.round( new Date() / 1000 ),
				fileFormat : "jpeg",
				rotation : rotation
			},
			function error ( err ) {
				console.log( "err: ", err );
			}
		).then(
			function success ( imageData ) {
				// Simulate a shutter closing.
				document.getElementById( 'viewfinder' ).removeAttribute( 'class' );
				new Audio('/audio/shutter.opus').play();

				App.persistentVar( 'last-photo', imageData );
				
				Views.show( 'confirm' );
			},
			function failure( err ) {
				console.log( err );

				Views.show( 'capture' );
			}
		);
	},

	alignViewfinder : function () {
		console.log( "Camera.alignViewfinder()" );

		var madeAChange = false;
		
		var positions = {
			'top' : 0,
			'left' : 0,
			'right' : 0,
			'bottom' : 0,
		};

		if ( screen.mozOrientation == 'portrait-primary' ) {
			positions.top = document.getElementById( 'viewfinder' ).getAttribute( 'width' ) || 0;
		}
		else if ( screen.mozOrientation == 'portrait-secondary' ) {
			positions.left = document.getElementById( 'viewfinder' ).getAttribute( 'height' ) || 0;
		}

		for ( var position in positions ) {
			if ( document.getElementById( 'viewfinder' ).style[position] != positions[position] + 'px' ) {
				console.log( position + " was " + document.getElementById( 'viewfinder' ).style[position] + ", not " + positions[position] + 'px' );
				
				document.getElementById( 'viewfinder' ).style[position] = positions[position] + 'px';
				madeAChange = true;
			}
		}

		if ( ! madeAChange && Views.viewSpecificTimers['alignViewfinder'] ) {
			clearTimeout( Views.viewSpecificTimers['alignViewfinder'] );
		}
	},

	autofocus : function () {
		console.log( "Camera.autofocus()" );

		Camera.cameraObj.autoFocus().then( function () {
			console.log( "Focus success." );
		}, function () {
			console.log( "Focus failure." );
		} );
	},

	chooseOriginalPhoto : function () {
		console.log( "Camera.chooseOriginalPhoto()" );

		$( '#choose-photo' ).on( 'change', function () {
			App.loading();

			var reader = new FileReader();
			reader.readAsDataURL( this.files[0] );
			reader.onloadend = function() {
				App.persistentVar( 'original-photo-data-url', reader.result );

				Views.show( 'capture' );
			};
		} );

		$( '#choose-photo' ).click();
	},

	getCamera : function () {
		return new Promise( function ( resolve, reject ) {
			if ( Camera.cameraObj ) {
				Camera.cameraObj.release();
				Camera.cameraObj = null;
			}

			var cameraManager = navigator.mozCameras;
			var availableCameras = cameraManager.getListOfCameras();

			if ( availableCameras.length > 1 ) {
				document.getElementById( 'camera-switch' ).style.display = '';
			}
			else {
				document.getElementById( 'camera-switch' ).style.display = 'none';
			}

			Camera.cameraCount = availableCameras.length;

			if ( availableCameras.length > 0 ) {
				cameraManager.getCamera(
					availableCameras[Camera.cameraIndex],
					{
						mode : 'picture'
					}
				).then( function success ( cameraControl ) {
					console.log( "Camera retrieval succeeded." );
					resolve( cameraControl );
				}, function error ( err ) {
					reject( "Another app is using the camera." );
				});
			}
			else {
				// @todo No cameras.
				reject( "No camera available." );
			}
		} );
	}
};

window.addEventListener( 'DOMContentLoaded', function () {
	console.log( "event: window.DOMContentLoaded" );

	document.getElementById( 'screen-intro' ).addEventListener( 'click', function () {
		console.log( "event: screen-intro.click" );

		Camera.chooseOriginalPhoto();
	} );

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
		
		var pics = navigator.getDeviceStorage( 'pictures' );
		var request = pics.addNamed( App.persistentVar( 'last-photo' ), "reenact-" + Date.now() + ".jpg" );

		request.onsuccess = function () {
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
				
						var pics = navigator.getDeviceStorage( 'pictures' );
						var request = pics.addNamed( blob, "reenact-merged-" + Date.now() + ".jpg" );
				
						request.onsuccess = function () {
							console.log( "addNamed succeeded" );
					
							Views.show( 'next-step' );
						};

						request.onerror = function () {
							console.log( "addNamed failed, ",  this.error );
					
							var errorName = this.error.name;
					
							navigator.mozL10n.formatValue( "generic-error", { 'error' : errorName } ).then( (string) => {
								alert(string);
							} );
						};
					}, "image/jpeg" );
				};
		
				newImageEl.setAttribute( 'src', newImageDataURL );
			};
	
			oldImageEl.setAttribute( 'src', oldImageDataURL );
		};

		request.onerror = function () {
			var errorName = this.error.name;
			
			navigator.mozL10n.formatValue( "generic-error", { 'error' : errorName } ).then( (string) => {
				alert( string );
			} );
			
			console.log( this.error );
		};
	} );
	
	document.getElementById( 'cancel-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: cancel-button.click" );

		Views.show( 'capture' );
	} );

	document.getElementById( 'share-button' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: share-button.click" );

		var activity = new MozActivity({
			name: "share",
			data: {
				type: "image/*",
				number: 1,
				blobs: [ App.persistentVar( 'final-photo-blob' ) ],
				filenames: 'reenact.jpg',
				filepaths: 'reenact.jpg',
			}
		});
	} );

	document.getElementById( 'shutter-release' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: shutter-release.click" );

		Camera.capture();
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

	document.getElementById( 'viewfinder' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: viewfinder.click" );
		Camera.autofocus();
	} );

	document.getElementById( 'original-photo' ).addEventListener( 'click', function ( evt ) {
		console.log( "event: original-photo.click" );
		Camera.autofocus();
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