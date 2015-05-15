/************************************************************************************

Filename    :   VrScene.cpp
Content     :   Trivial game style scene viewer VR sample
Created     :   September 8, 2013
Authors     :   John Carmack

Copyright   :   Copyright 2012 Oculus VR, LCC. All Rights reserved.

*************************************************************************************/

#include "VrScene.h"

static const char * versionString = "VrScene v.0.4.2\n"__DATE__"\n"__TIME__;

extern "C"
{

long Java_com_oculusvr_vrscene_MainActivity_nativeSetAppInterface( JNIEnv *jni, jclass clazz, jobject activity,
	jstring fromPackageName, jstring commandString, jstring uriString )
{
	// This is called by the java UI thread.
	LOG( "nativeSetAppInterface" );
	return (new VrScene())->SetActivity( jni, clazz, activity, fromPackageName, commandString, uriString );
}

} // extern "C"

//=============================================================================
//                             VrScene
//=============================================================================

VrScene::VrScene() :
	forceScreenClear( false ),
	ModelLoaded( false )
{
}

VrScene::~VrScene() {
	LOG( "~VrScene()");
}

void VrScene::ConfigureVrMode( ovrModeParms & modeParms )
{
	modeParms.CpuLevel = 2;
	modeParms.GpuLevel = 2;

	// Always use 2x MSAA for now
	app->GetVrParms().multisamples = 2;
}

void VrScene::OneTimeInit( const char * fromPackageName, const char * launchIntentJSON, const char * launchIntentURI )
{
	LOG( "VrScene::OneTimeInit" );

	app->GetStoragePaths().PushBackSearchPathIfValid(EST_SECONDARY_EXTERNAL_STORAGE, EFT_ROOT, "RetailMedia/", SearchPaths);
	app->GetStoragePaths().PushBackSearchPathIfValid(EST_SECONDARY_EXTERNAL_STORAGE, EFT_ROOT, "", SearchPaths);
	app->GetStoragePaths().PushBackSearchPathIfValid(EST_PRIMARY_EXTERNAL_STORAGE, EFT_ROOT, "RetailMedia/", SearchPaths);
	app->GetStoragePaths().PushBackSearchPathIfValid(EST_PRIMARY_EXTERNAL_STORAGE, EFT_ROOT, "", SearchPaths);

	// Check if we already loaded the model through an intent
	if ( !ModelLoaded )
	{
		LoadScene( launchIntentURI );
	}
}

void VrScene::OneTimeShutdown()
{
	LOG( "VrScene::OneTimeShutdown" );

	// Free GL resources
}

void VrScene::NewIntent( const char * fromPackageName, const char * command, const char * uri )
{
	LOG( "NewIntent - fromPackageName : %s, command : %s, uri : %s", fromPackageName, command, uri );

	// Scene will be loaded in "OneTimeInit" function.
	// LoadScene( intent );
}

// uncomment this to make the intent load a test model instead of change the scene.
// we may just want to make the intent consist of <command> <parameters>.
//#define INTENT_TEST_MODEL

void VrScene::LoadScene( const char * path )
{
	LOG( "VrScene::LoadScene %s", path );

#if defined( INTENT_TEST_MODEL )
	const char * scenePath = "Oculus/tuscany.ovrscene";
#else
	const char * scenePath = ( path[0] != '\0' ) ? path : "Oculus/tuscany.ovrscene";
#endif
	if ( !GetFullPath( SearchPaths, scenePath, SceneFile ) )
	{
		LOG( "VrScene::NewIntent SearchPaths failed to find %s", scenePath );
	}

	MaterialParms materialParms;
	materialParms.UseSrgbTextureFormats = ( app->GetVrParms().colorFormat == COLOR_8888_sRGB );
	LOG( "VrScene::LoadScene loading %s", SceneFile.ToCStr() );
	Scene.LoadWorldModel( SceneFile, materialParms );
	ModelLoaded = true; 
	LOG( "VrScene::LoadScene model is loaded" );
	Scene.YawOffset = -M_PI / 2;

#if defined( INTENT_TEST_MODEL )
	// load a test model
	const char * testModelPath = intent;
	if ( testModelPath != NULL && testModelPath[0] != '\0' )
	{
		// Create the render programs we are going to use
		GlProgram ProgSingleTexture = BuildProgram( SingleTextureVertexShaderSrc,
				SingleTextureFragmentShaderSrc );

		ModelGlPrograms programs( &ProgSingleTexture );

		TestObject.SetModelFile( LoadModelFile( testModelPath, programs, materialParms ) );
		Scene.AddModel( &TestObject );
	}
#endif

	// When launched by an intent, we may be viewing a partial
	// scene for debugging, so always clear the screen to grey
	// before drawing, instead of letting partial renders show through.
	forceScreenClear = ( path[0] != '\0' );
}

void VrScene::ReloadScene()
{
	// Reload the scene, presumably to switch texture formats
	const Vector3f pos = Scene.FootPos;
	const float	yaw = Scene.YawOffset;

	MaterialParms materialParms;
	materialParms.UseSrgbTextureFormats = ( app->GetVrParms().colorFormat == COLOR_8888_sRGB );
	Scene.LoadWorldModel( SceneFile, materialParms );

	Scene.YawOffset = yaw;
	Scene.FootPos = pos;
}

void VrScene::Command( const char * msg )
{
}

Matrix4f VrScene::DrawEyeView( const int eye, const float fovDegrees )
{
	if ( forceScreenClear )
	{
		glClearColor( 0.25f, 0.25f, 0.25f, 1.0f );
		glClear( GL_COLOR_BUFFER_BIT );
	}

	const Matrix4f view = Scene.DrawEyeView( eye, fovDegrees );

	return view;
}

Matrix4f VrScene::Frame( const VrFrame vrFrame )
{
	// Get the current vrParms for the buffer resolution.
	const EyeParms vrParms = app->GetEyeParms();

	// Player movement
	Scene.Frame( app->GetVrViewParms(), vrFrame, app->GetSwapParms().ExternalVelocity );

	// Make the test object hop up and down
	{
		const float y = 1 + sin( 2 * vrFrame.PoseState.TimeInSeconds );
		TestObject.State.modelMatrix.M[0][3] = 2;
		TestObject.State.modelMatrix.M[1][3] = y;
	}

	// these should probably use OnKeyEvent() now so that the menu can just consume the events
	// if it's open, rather than having an explicit check here.
	if ( !app->IsGuiOpen() )
	{
		//-------------------------------------------
		// Check for button actions
		//-------------------------------------------
		if ( !( vrFrame.Input.buttonState & BUTTON_RIGHT_TRIGGER ) )
		{
			if ( vrFrame.Input.buttonPressed & BUTTON_SELECT )
			{
				app->CreateToast( "%s", versionString );
			}

			// Switch buffer parameters for testing
			if ( vrFrame.Input.buttonPressed & BUTTON_X )
			{
				EyeParms newParms = vrParms;
				switch ( newParms.multisamples )
				{
					case 2: newParms.multisamples = 4; break;
					case 4: newParms.multisamples = 1; break;
					default: newParms.multisamples = 2; break;
				}
				app->SetEyeParms( newParms );
				app->CreateToast( "multisamples: %i", newParms.multisamples );
			}
		}
	}

	//-------------------------------------------
	// Render the two eye views, each to a separate texture, and TimeWarp
	// to the screen.
	//-------------------------------------------
	app->DrawEyeViewsPostDistorted( Scene.CenterViewMatrix() );

	return Scene.CenterViewMatrix();
}
