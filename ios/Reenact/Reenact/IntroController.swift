//
//  IntroController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class IntroController: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    // MARK: Properties
    
    let picker = UIImagePickerController()
    
    var originalPhoto:UIImage?
    
    let choosePhotoButton: UIButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        picker.delegate = self
        
        // Add the logo on the intro screen.
        let reenactLogo = UIImage(named:"logo.png")
        let reenactLogoView = UIImageView(image: reenactLogo)
        reenactLogoView.contentMode = .ScaleAspectFit
        
        let reenactLogoWidth = round( min( view.bounds.size.width, view.bounds.size.height ) / 3 )
        reenactLogoView.frame = CGRect(
            x: round((view.bounds.size.width / 2) - (reenactLogoWidth / 2)),
            y: round(view.bounds.size.height / 3),
            width: reenactLogoWidth,
            height: reenactLogoWidth
        )
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
        }
        else {
            // Landscape orientation.

        }
        
        view.addSubview(reenactLogoView)
        
        choosePhotoButton.setTitle("CHOOSE A PHOTO TO REENACT", forState: .Normal)
        choosePhotoButton.setTitleColor(UIColor.whiteColor(), forState: .Normal)
        choosePhotoButton.backgroundColor = UIColor.blackColor()
        choosePhotoButton.contentEdgeInsets = UIEdgeInsetsMake(7.0, 7.0, 7.0, 7.0)
        choosePhotoButton.addTarget(self, action:"chooseOriginalPhoto:", forControlEvents: .TouchUpInside)
        choosePhotoButton.titleLabel!.font = UIFont(name:"Helvetica Neue", size: 12)
        choosePhotoButton.frame = CGRect(
            x: round((view.bounds.width - 240) / 2),
            y: round(view.bounds.height * 0.85),
            width: 240,
            height: 30
        )
        // @todo Use auto-layout to center this button horizontally and size it vertically and horizontally.
        
        view.addSubview(choosePhotoButton)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        if (segue.identifier == "introToCapture") {
            print("In prepareForSegue")
            let svc = segue.destinationViewController as! CaptureController;
            
            svc.originalPhoto = self.originalPhoto
        }
    }

    func chooseOriginalPhoto(sender: UIButton!) {
        print("hi")
        
        picker.allowsEditing = false
        picker.sourceType = .PhotoLibrary
        presentViewController(picker, animated: true, completion: nil)
        
    }
    
    //MARK: Delegates
    func imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [String : AnyObject])
    {
        let chosenImage = info[UIImagePickerControllerOriginalImage] as! UIImage //2
        //myImageView.contentMode = .ScaleAspectFit //3
        //myImageView.image = chosenImage //4
        dismissViewControllerAnimated(true, completion: nil) //5
        
        // Show the next view and set the camera background image to chosenImage
        self.originalPhoto = chosenImage
        self.performSegueWithIdentifier("introToCapture", sender: self)
        
    }
    
    func imagePickerControllerDidCancel(picker: UIImagePickerController) {
        dismissViewControllerAnimated(true, completion: nil)
      
    }
    
}

