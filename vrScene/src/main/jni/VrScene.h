/************************************************************************************

Filename    :   VrScene.h
Content     :   Trivial game style scene viewer VR sample
Created     :   September 8, 2013
Authors     :   John Carmack

Copyright   :   Copyright 2014 Oculus VR, LLC. All Rights reserved.

************************************************************************************/

#ifndef VRSCENE_H
#define VRSCENE_H

#include "App.h"
#include "ModelView.h"

class VrScene : public OVR::VrAppInterface
{
public:
						VrScene();
						~VrScene();

	virtual void 		ConfigureVrMode( ovrModeParms & modeParms );
	virtual void		OneTimeInit( const char * fromPackage, const char * launchIntentJSON, const char * launchIntentURI );
	virtual void		OneTimeShutdown();
	virtual Matrix4f	DrawEyeView( const int eye, const float fovDegrees );
	virtual Matrix4f	Frame( VrFrame vrFrame );
	virtual	void		NewIntent( const char * fromPackageName, const char * command, const char * uri );
	virtual void		Command( const char * msg );

	void				LoadScene( const char * path );
    void				ReloadScene();

	// When launched by an intent, we may be viewing a partial
	// scene for debugging, so always clear the screen to grey
	// before drawing, instead of letting partial renders show through.
	bool				forceScreenClear;

	bool				ModelLoaded;

	String				SceneFile;	// for reload
	OvrSceneView		Scene;

	ModelInScene		TestObject;		// bouncing object

	Array<String> 		SearchPaths;
};

#endif
