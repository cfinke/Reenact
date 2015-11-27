//
//  ViewController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ViewController: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    // MARK: Properties
    
    @IBOutlet weak var choosePhotoButton: UIButton!
    
    let picker = UIImagePickerController()
    
    var originalPhoto:UIImage?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        picker.delegate = self
        
        // Set up the aspects of the picker button that couldn't
        // be done via the storyboard.
        choosePhotoButton.backgroundColor = UIColor.blackColor()
        choosePhotoButton.contentEdgeInsets = UIEdgeInsetsMake(7.0, 7.0, 7.0, 7.0)
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

    
    // MARK: Actions    
    @IBAction func chooseOriginalPhoto(button: UIButton) {
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

