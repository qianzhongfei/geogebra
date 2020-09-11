package org.geogebra.web.html5.main;

import com.google.gwt.core.client.JavaScriptObject;

import elemental2.promise.Promise;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

/**
 * Maps GeoGebra functions to exported JS api
 */
@JsType
public class ApiExporter {

	private GgbAPIW ggbAPI;

	public ApiExporter(GgbAPIW ggbAPI) {
		this.ggbAPI = ggbAPI;
	}

	public String getXML(String objName) {
		if (Js.isTruthy(String objName)) {
			return ggbAPI.getXML(objName + "");
		} else {
			return ggbAPI.getXML();
		}
	}

	public String getAlgorithmXML(String objName) {
		return ggbAPI.getAlgorithmXML(objName + "");
	}

	public String getPerspectiveXML() {
		return ggbAPI.getPerspectiveXML();
	}

	public String getBase64(JavaScriptObject param1, JavaScriptObject param2) {
		if (Js.isTripleEqual(param2, false)) {
			return ggbAPI.getBase64(false);
		}
		if (Js.isTripleEqual(param2, true)) {
			return ggbAPI.getBase64(true);
		}
		if (Js.isTruthy(param2)) {
			ggbAPI.getBase64(Js.asBoolean(param1), param2);
		} else if (Js.isTruthy(param1)) {
			ggbAPI.getBase64(false, param1);
		} else {
			return ggbAPI.getBase64();
		}

		return null;
	}

	public void setBase64(String base64string, JavaScriptObject callback) {
		ggbAPI.setBase64(base64string + "", callback);
	}

	public void openFile(String filename, JavaScriptObject callback) {
		ggbAPI.openFile(filename + "", callback);
	};

	public void login(String token, Object ui) {
		ggbAPI.login(token  + "", Js.asBoolean(ui));
	}

	public void logout() {
		ggbAPI.logout();
	};

	public void setXML(String xml) {
		ggbAPI.setXML(xml + "");
	}

	public void evalXML(String xmlString) {
		ggbAPI.evalXML(xmlString + "");
	}

	public void setDisplayStyle(String objName, String style) {
		ggbAPI.setDisplayStyle(objName + "", style + "");
	}

	public boolean evalCommand(String cmdString) {
		return ggbAPI.evalCommandNoException(cmdString + "");
	}

	public String evalCommandGetLabels(String cmdString) {
		return ggbAPI.evalCommandGetLabelsNoException(cmdString + "");
	}

	public Promise<String> asyncEvalCommand(String cmdString) {
		return new Promise<>((resolve, reject) -> {
			ggbAPI.asyncEvalCommand(cmdString + "", resolve, reject);
		});
	};

	public Promise<String> asyncEvalCommandGetLabels(String cmdString) {
		return new Promise<>((resolve, reject) -> {
			ggbAPI.asyncEvalCommandGetLabels(cmdString + "", resolve, reject);
		});
	};

	public String evalCommandCAS(String cmdString) {
		return ggbAPI.evalCommandCAS(cmdString + "");
	}

	public String evalGeoGebraCAS(String cmdString) {
		return ggbAPI.evalGeoGebraCAS(cmdString + "");
	}

	public void setFixed(String objName, Object flag, Object selection) {
		if ("undefined".equals(Js.typeof(selection))) {
			ggbAPI.setFixed(objName + "", Js.asBoolean(flag));
		} else {
			ggbAPI.setFixed(objName + "", Js.asBoolean(flag), Js.asBoolean(selection));
		}
	};

	public void setOnTheFlyPointCreationActive(Object flag) {
		ggbAPI.setOnTheFlyPointCreationActive(Js.asBoolean(flag));
	}

	public void setUndoPoint() {
		ggbAPI.setUndoPoint();
	}

	public void setSaved() {
		ggbAPI.setSaved();
	};

	public void initCAS() {
		ggbAPI.initCAS();
	};

	public void uploadToGeoGebraTube() {
		ggbAPI.uploadToGeoGebraTube();
	};

	public void setErrorDialogsActive(Object flag) {
		ggbAPI.setErrorDialogsActive(Js.asBoolean(flag));
	}

	public void reset() {//TODO: implement this in Desktop and Web
		ggbAPI.reset();
	}

	public void refreshViews() {
		ggbAPI.refreshViews();
	}

	public void setVisible(String objName, Object visible) {
		ggbAPI.setVisible(objName + "", Js.asBoolean(visible));
	}

	public boolean getVisible(String objName, Object view) {
		if (!"undefined".equals(Js.typeof(view))) {
			return ggbAPI.getVisible(objName + "", Js.asInt(view));
		}
		return ggbAPI.getVisible(objName + "");
	};

	public void setLayer(String objName, int layer) {
		ggbAPI.setLayer(objName + "", layer);
	}

	public int getLayer(String objName) {
		return ggbAPI.getLayer(objName + "");
	}

	public void setLayerVisible(int layer, Object visible) {
		ggbAPI.setLayerVisible(layer, Js.asBoolean(visible));
	}

	public void setTrace(String objName, Object flag) {
		ggbAPI.setTrace(objName + "", Js.asBoolean(flag));
	}

	public boolean isTracing(String objName) {
		return ggbAPI.isTracing(objName + "");
	}

	public void setLabelVisible(String objName, Object visible) {
		ggbAPI.setLabelVisible(objName + "", Js.asBoolean(visible));
	}

	public void setLabelStyle(String objName, int style) {
		ggbAPI.setLabelStyle(objName + "", style);
	}

	public int getLabelStyle(String objName) {
		return ggbAPI.getLabelStyle(objName + "");
	};

	public void getLabelVisible(String objName) {
		ggbAPI.getLabelVisible(objName + "");
	}

	public void setColor(String objName, int red, int green, int blue) {
		ggbAPI.setColor(objName + "", red, green, blue);
	}

	public void setCorner(String objName, double x, double y, int index) {
		if (Js.isFalsy(index)) {
			index = 1;
		}
		ggbAPI.setCorner(objName + "", x, y, index);
	}

	public void setLineStyle(String objName, int style) {
		ggbAPI.setLineStyle(objName + "", style);
	}

	public void setLineThickness(String objName, int thickness) {
		ggbAPI.setLineThickness(objName + "", thickness);
	}

	public void setPointStyle(String objName, int style) {
		ggbAPI.setPointStyle(objName + "", style);
	}

	public void setPointSize(String objName, int style) {
		ggbAPI.setPointSize(objName + "", style);
	}

	public void setFilling(String objName, double filling) {
		ggbAPI.setFilling(objName + "", filling);
	}

	public String getColor(String objName) {
		return ggbAPI.getColor(objName + "");
	}

	public String getPenColor() {
		return ggbAPI.getPenColor();
	};

	public int getPenSize() {
		return ggbAPI.getPenSize();
	};

	public void setPenSize(int size) {
		ggbAPI.setPenSize(size);
	}

	public void setPenColor(int red, int green, int blue) {
		ggbAPI.setPenColor(red, green, blue);
	}

	public double getFilling(String objName) {
		return ggbAPI.getFilling(objName + "");
	}

	public String getImageFileName(String objName) {
		return ggbAPI.getImageFileName(objName + "");
	}

	public int getLineStyle(String objName) {
		return ggbAPI.getLineStyle(objName + "");
	}

	public int getLineThickness(String objName) {
		return ggbAPI.getLineThickness(objName + "");
	}

	public int getPointStyle(String objName) {
		return ggbAPI.getPointStyle(objName + "");
	}

	public int getPointSize(String objName) {
		return ggbAPI.getPointSize(objName + "");
	}

	public void deleteObject(String objName) {
		ggbAPI.deleteObject(objName + "");
	}

	public void setAnimating(String objName, Object animate) {
		ggbAPI.setAnimating(objName + "", Js.asBoolean(animate));
	}

	public void setAnimationSpeed(String objName, double speed) {
		ggbAPI.setAnimationSpeed(objName + "", speed);
	}

	public void startAnimation() {
		ggbAPI.startAnimation();
	}

	public void stopAnimation() {
		ggbAPI.stopAnimation();
	}

	public void setAuxiliary(String objName, Object auxiliary) {
		ggbAPI.setAuxiliary(objName + "", Js.asBoolean(auxiliary));
	}

	public void hideCursorWhenDragging(Object hideCursorWhenDragging) {
		ggbAPI.hideCursorWhenDragging(Js.asBoolean(hideCursorWhenDragging));
	}

	public boolean isAnimationRunning() {
		return ggbAPI.isAnimationRunning();
	}

	public double getFrameRate() {
		return ggbAPI.getFrameRate();
	}

	public boolean renameObject(String oldName, String newName, Object force) {
		return ggbAPI.renameObject(oldName + "",newName + "", Js.asBoolean(force));
	}

	public boolean exists(String objName) {
		return ggbAPI.exists(objName + "");
	}

	public boolean isDefined(String objName) {
		return ggbAPI.isDefined(objName + "");
	}

	public String getValueString(String objName, Object localized) {
		boolean localizedB = "undefined".equals(Js.typeof(localized)) || Js.asBoolean(localized);
		return ggbAPI.getValueString(objName + "", localizedB);
	}

	public double getListValue(String objName, int index) {
		return ggbAPI.getListValue(objName + "", index);
	}

	public String getDefinitionString(String objName, Object localized) {
		boolean localizedB = "undefined".equals(Js.typeof(localized)) || Js.asBoolean(localized);
		return ggbAPI.getDefinitionString(objName + "",  localizedB);
	}

	public String getLaTeXString(String objName) {
		return ggbAPI.getLaTeXString(objName + "");
	}

	public String getLaTeXBase64(String objName, Object value) {
		return ggbAPI.getLaTeXBase64(objName + "", Js.asBoolean(value));
	}

	public String getCommandString(String objName, Object localized) {
		boolean localizedB = "undefined".equals(Js.typeof(localized)) || Js.asBoolean(localized);
		return ggbAPI.getCommandString(objName + "", localizedB);
	}

	public String getCaption(String objName, subst) {
		return ggbAPI.getCaption(Ljava/lang/String;Z)(objName + "", Js.asBoolean(subst));
	};

	public String setCaption(String objName, caption) {
		ggbAPI.setCaption(Ljava/lang/String;Ljava/lang/String;)(objName + "", caption + "");
	};

	public String getXcoord(String objName) {
		return ggbAPI.getXcoord(objName + "");
	};

	public String getYcoord(String objName) {
		return ggbAPI.getYcoord(objName + "");
	};

	public String getZcoord(String objName) {
		return ggbAPI.getZcoord(objName + "");
	};

	public String setCoords(String objName, x, y, z) {
		if (typeof z === 'undefined') {
			ggbAPI.setCoords(Ljava/lang/String;DD)(objName + "",x,y);
		} else {
			ggbAPI.setCoords(Ljava/lang/String;DDD)(objName + "",x,y,z);
		}
	};

	public String getValue(String objName) {
		return ggbAPI.getValue(objName + "");
	};

	public String getVersion() {
		return ggbAPI.getVersion()();
	};

	public String getScreenshotBase64(callback) {
		ggbAPI.getScreenshotBase64(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
	};

	public String getThumbnailBase64() {
		return ggbAPI.getThumbnailBase64()();
	};

	public String setValue(String objName, x) {
		// #4035
		// need to support possible syntax error
		// eg setValue("a","3") rather than setValue("a",3)
		if (typeof x === "string") {
			if (x === "true") {
				x = true;
			} else if (x === "false") {
				x = false;
			} else {
				// force string -> number (might give NaN)
				x = x * 1;
			}
		}

		if (typeof x !== "number" && typeof x !== "boolean") {
			// avoid possible strange effects
			return;
		}
		ggbAPI.setValue(Ljava/lang/String;D)(objName + "",x);
	};

	public String setTextValue(String objName, x) {

		x = x + "";

		if (typeof objName !== "string") {
			// avoid possible strange effects
			return;
		}
		ggbAPI.setTextValue(Ljava/lang/String;Ljava/lang/String;)(objName + "",x);
	};

	public String setListValue(String objName, x, y) {
		// #4035
		// need to support possible syntax error
		if (typeof x === "string") {
			if (x === "true") {
				x = 1;
			} else if (x === "false") {
				x = 0;
			} else {
				// force string -> number (might give NaN)
				x = x * 1;
			}
		}

		if (typeof y === "string") {
			if (y === "true") {
				y = 1;
			} else if (y === "false") {
				y = 0;
			} else {
				// force string -> number (might give NaN)
				y = y * 1;
			}
		}

		if (typeof x !== "number" || typeof y !== "number") {
			// avoid possible strange effects
			return;
		}
		ggbAPI.setListValue(Ljava/lang/String;DD)(objName + "",x,y);
	};

	public String setRepaintingActive(flag) {
		ggbAPI.setRepaintingActive(Z)(Js.asBoolean(flag));
	};

	public String setCoordSystem(xmin, xmax, ymin, ymax, zmin, zmax,
			verticalY) {
		if (typeof zmin !== "number") {
			ggbAPI.setCoordSystem(DDDD)(xmin,xmax,ymin,ymax);
		} else {
			ggbAPI.setCoordSystem(DDDDDDZ)(xmin,xmax,ymin,ymax,zmin,zmax, Js.asBoolean(verticalY));
		}
	};

	public String setAxesVisible(arg1, arg2, arg3, arg4) {
		if (typeof arg3 === "undefined") {
			ggbAPI.setAxesVisible(ZZ)(Js.asBoolean(arg)1, Js.asBoolean(arg)2);
		} else {
			ggbAPI.setAxesVisible(IZZZ)(arg1, Js.asBoolean(arg)2, Js.asBoolean(arg)3, Js.asBoolean(arg)4);
		}
	};

	public String setAxisUnits(arg1, arg2, arg3, arg4) {
		ggbAPI.setAxisUnits(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)(arg1, arg2 + "", arg3 + "", arg4 + "");
	};

	public String setAxisLabels(arg1, arg2, arg3, arg4) {
		ggbAPI.setAxisLabels(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)(arg1, arg2 + "", arg3 + "", arg4 + "");
	};

	public String setAxisSteps(arg1, arg2, arg3, arg4) {
		ggbAPI.setAxisSteps(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)(arg1, arg2 + "",arg3 + "", arg4 + "");
	};

	api.getAxisUnits = $entry(function(arg1) {
		return []
				.concat(ggbAPI.getAxisUnits(I)(1 * arg1 || 1));
	});

	api.getAxisLabels = $entry(function(arg1) {
		return []
				.concat(ggbAPI.getAxisLabels(I)(1 * arg1 || 1));
	});

	public String setPointCapture(view, capture) {
		if (typeof capture === "undefined") {
			ggbAPI.setPointCapture(II)(1, view);
		} else {
			ggbAPI.setPointCapture(II)(view, capture);
		}
	};

	public String getGridVisible(view) {
		return ggbAPI.getGridVisible(I)(view || 1);
	};

	public String setGridVisible(arg1, arg2) {
		if (typeof arg2 === "undefined") {
			ggbAPI.setGridVisible(Z)(Js.asBoolean(arg)1);
		} else {
			ggbAPI.setGridVisible(IZ)(arg1, Js.asBoolean(arg)2);
		}
	};

	public String getAllObjectNames(objectType) {
		if (typeof objectType === "undefined") {
			return []
					.concat(ggbAPI.getAllObjectNames()());
		} else {
			return []
					.concat(ggbAPI.getAllObjectNames(objectType + ""));
		}

	};

	public String getObjectNumber() {
		return ggbAPI.getObjectNumber()();
	};

	public String getObjectName(i) {
		return ggbAPI.getObjectName(I)(i);
	};

	public String getObjectType(String objName) {
		return ggbAPI.getObjectType(objName + "");
	};

	public String setMode(mode) {
		ggbAPI.setMode(I)(mode);
	};

	public String getMode() {
		return ggbAPI.getMode()();
	};

	public String getToolName(i) {
		return ggbAPI.getToolName(I)(i);
	};

	public String openMaterial(material) {
		ggbAPI.openMaterial(material + "");
	};

	public String undo(repaint) {
		ggbAPI.undo(Z)(Js.asBoolean(repaint));
	};

	public String redo(repaint) {
		ggbAPI.redo(Z)(Js.asBoolean(repaint));
	};

	public String newConstruction() {
		ggbAPI.newConstruction()();
	};

	public String debug(str) {
		ggbAPI.debug(str + "");
	};

	public String setWidth(width) {
		ggbAPI.setWidth(I)(width);
	};

	public String setHeight(height) {
		ggbAPI.setHeight(I)(height);
	};

	public String setSize(width, height) {
		ggbAPI.setSize(II)(width, height);
	};

	public String enableRightClick(enable) {
		ggbAPI.enableRightClick(Z)(Js.asBoolean(enable));
	};

	public String enableLabelDrags(enable) {
		ggbAPI.enableLabelDrags(Z)(Js.asBoolean(enable));
	};

	public String enableShiftDragZoom(enable) {
		ggbAPI.enableShiftDragZoom(Z)(Js.asBoolean(enable));
	};

	public String showToolBar(show) {
		ggbAPI.showToolBar(Z)(Js.asBoolean(show));
	};

	public String setCustomToolBar(toolbarDef) {
		ggbAPI.setCustomToolBar(toolbarDef + "");
	};

	public String showMenuBar(show) {
		ggbAPI.showMenuBar(Z)(Js.asBoolean(show));
	};

	public String showAlgebraInput(show) {
		ggbAPI.showAlgebraInput(Z)(Js.asBoolean(show));
	};

	public String showResetIcon(show) {
		ggbAPI.showResetIcon(Z)(Js.asBoolean(show));
	};

	public String getViewProperties(show) {
		return ggbAPI.getViewProperties(I)(show);
	};

	public String setFont(label, size, bold, italic, serif) {
		ggbAPI.setFont(Ljava/lang/String;IZZZ)(label + "", size, Js.asBoolean(bold), Js.asBoolean(italic), Js.asBoolean(serif));
	};

	public String insertImage(url, corner1, corner2, corner4) {
		return ggbAPI.insertImage(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(url + "", corner1+"", corner2+"", corner4+"");
	};

	public String addImage(fileName, url) {
		ggbAPI.addImage(*)(fileName + "", url + "");
	};

	public String recalculateEnvironments() {
		ggbAPI.recalculateEnvironments()();
	};

	public String isIndependent(label) {
		return ggbAPI.isIndependent(label + "");
	};

	public String isMoveable(label) {
		return ggbAPI.isMoveable(label + "");
	};

	public String setPerspective(code) {
		ggbAPI.setPerspective(code + "");
	};

	public String enableCAS(enable) {
		ggbAPI.enableCAS(Z)(Js.asBoolean(enable));
	};

	public String enable3D(enable) {
		ggbAPI.enable3D(Z)(Js.asBoolean(enable));
	};

	public String getPNGBase64(exportScale, transparent, dpi,
			copyToClipboard, greyscale) {
		return ggbAPI.getPNGBase64(DZDZZ)(exportScale, Js.asBoolean(transparent), dpi, Js.asBoolean(copyToClipboard), Js.asBoolean(greyscale));
	};

	public String exportGIF(sliderLabel, scale, timeBetweenFrames, isLoop, filename, rotate) {
		return ggbAPI.exportGIF(Ljava/lang/String;DDZLjava/lang/String;D)(sliderLabel, scale, timeBetweenFrames | 500, Js.asBoolean(isLoop), filename, rotate | 0);
	};

	public String getFileJSON(thumbnail) {
		return ggbAPI.getFileJSON(Z)(Js.asBoolean(thumbnail));
	};

	public String setFileJSON(zip) {
		return ggbAPI.setFileJSON(Ljava/lang/Object;)(zip);
	};

	public String setLanguage(lang) {
		return ggbAPI.setLanguage(lang + "");
	};

	public String showTooltip(lang) {
		return ggbAPI.showTooltip(lang + "");
	};

	// APPS-646 deprecated, needs changing to getValue("correct")
	api.getExerciseFraction = function() {
		return ggbAPI.@org.geogebra.common.plugin.GgbAPI::getExerciseFraction()();
	};

	// APPS-646 Exercises no longer supported
	api.isExercise = function() {
		return false;
	};

	public String setExternalPath(path) {
		ggbAPI.setExternalPath(path + "");
	};

	public String checkSaved(path) {
		ggbAPI.checkSaved(Lcom/google/gwt/core/client/JavaScriptObject;)(path);
	};

	public String getCASObjectNumber() {
		return ggbAPI.getCASObjectNumber()();
	};

	public String writePNGtoFile(filename, exportScale, transparent, DPI, greyscale) {
		return ggbAPI.writePNGtoFile(Ljava/lang/String;DZDZ)(filename + "", exportScale, Js.asBoolean(transparent), DPI, Js.asBoolean(greyscale));
	};

	public String exportPGF(callback) {
		return ggbAPI.exportPGF(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
	};

	public String exportSVG(filename) {
		return ggbAPI.exportSVG(filename);
	};

	public String exportPDF(scale, filename, sliderLabel) {
		return ggbAPI.exportPDF(DLjava/lang/String;Ljava/lang/String;)(scale | 1, filename, sliderLabel);
	};

	public String exportPSTricks(callback) {
		return ggbAPI.exportPSTricks(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
	};

	public String exportAsymptote(callback) {
		return ggbAPI.exportAsymptote(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
	};

	public String setRounding(digits) {
		return ggbAPI.setRounding(digits + "");
	};

	public String getRounding() {
		return ggbAPI.getRounding()();
	};

	public String copyTextToClipboard(text) {
		return ggbAPI.copyTextToClipboard(text + "");
	};

	public String evalLaTeX(text,mode) {
		return ggbAPI.evalLaTeX(text + "", mode);
	};

	public String evalMathML(text) {
		return ggbAPI.evalMathML(text + "");
	};

	public String getScreenReaderOutput(text) {
		return ggbAPI.getScreenReaderOutput(text + "");
	};

	public String getEditorState() {
		return ggbAPI.getEditorState()();
	};

	public String setEditorState(state, label) {
		var stateString = typeof state == "string" ? state : JSON.stringify(state);
		ggbAPI.setEditorState(Ljava/lang/String;Ljava/lang/String;)(stateString, label || "");
	};

	public String exportCollada(xmin, xmax, ymin, ymax, zmin, zmax,
				xyScale, xzScale, xTickDistance, yTickDistance, zTickDistance) {
		return ggbAPI.exportCollada(DDDDDDDDDDD)(
			xmin || -5, xmax || 5, ymin || -5, ymax || 5, zmin || -5, zmax || 5, xyScale || 1,
			xzScale || 1, xTickDistance || -1, yTickDistance || -1, zTickDistance || -1);
	};

	public String exportSimple3d(name, xmin, xmax, ymin, ymax, zmin, zmax,
				xyScale, xzScale, xTickDistance, yTickDistance, zTickDistance) {
		return ggbAPI.exportSimple3d(Ljava/lang/String;DDDDDDDDDDD)(
			name + "", xmin, xmax, ymin, ymax, zmin, zmax, xyScale,
			xzScale, xTickDistance, yTickDistance, zTickDistance);
	};

	public String translate(arg1, callback) {
		return ggbAPI.translate(arg1 + "", callback);
	};

	public String exportConstruction(flags) {
		return ggbAPI.exportConstruction(Lcom/google/gwt/core/client/JsArrayString;)(flags || ["color","name","definition","value"]);
	};

	public String getConstructionSteps(breakpoints) {
		return ggbAPI.getConstructionSteps(Z)(Js.asBoolean(breakpoints));
	};

	public String setConstructionStep(n, breakpoints) {
		ggbAPI.setConstructionStep(DZ)(n, Js.asBoolean(breakpoints));
	};

	public String previousConstructionStep() {
		ggbAPI.previousConstructionStep()();
	};

	public String nextConstructionStep() {
		ggbAPI.nextConstructionStep()();
	};

	public String getEmbeddedCalculators() {
		return ggbAPI.getEmbeddedCalculators()();
	};

	public String getFrame(){
		return ggbAPI.getFrame()();
	}

	public String enableFpsMeasurement() {
		ggbAPI.enableFpsMeasurement()();
	};

	public String disableFpsMeasurement() {
		ggbAPI.disableFpsMeasurement()();
	};

	public String testDraw() {
		ggbAPI.testDraw()();
	};

	public String startDrawRecording() {
		ggbAPI.startDrawRecording()();
	};

	public String endDrawRecordingAndLogResults() {
		ggbAPI.endDrawRecordingAndLogResults()();
	};
}

/**
 * @param api
 *            exported object
 * @param ggbAPI
 *            internal API
 * @param getId
 *            listener to ID mapping
 */
protected final native void addClientListener(JavaScriptObject api,
		GgbAPIW ggbAPI, JavaScriptObject getId) /*-{
	public String registerClientListener(JSFunctionName) {
		ggbAPI.registerClientListener(getId(JSFunctionName));
	};
}-*/;

/**
 * Adds client listeners for specific events (add, update, click, ...)
 */
private native void addSpecificListenerFunctionsNative(JavaScriptObject api,
		GgbAPIW ggbAPI, JavaScriptObject listenerMappingFunction) /*-{
	var getId = listenerMappingFunction;

	public String registerAddListener(JSFunctionName) {
		ggbAPI.registerAddListener(getId(JSFunctionName));
	};

	public String registerStoreUndoListener(JSFunctionName) {
		ggbAPI.registerStoreUndoListener(getId(JSFunctionName));
	};

	public String unregisterAddListener(JSFunctionName) {
		ggbAPI.unregisterAddListener(getId(JSFunctionName));
	};

	public String registerRemoveListener(JSFunctionName) {
		ggbAPI.registerRemoveListener(getId(JSFunctionName));
	};

	public String unregisterRemoveListener(JSFunctionName) {
		ggbAPI.unregisterRemoveListener(getId(JSFunctionName));
	};

	public String registerClearListener(JSFunctionName) {
		ggbAPI.registerClearListener(getId(JSFunctionName));
	};

	public String unregisterClearListener(JSFunctionName) {
		ggbAPI.unregisterClearListener(getId(JSFunctionName));
	};

	public String registerRenameListener(JSFunctionName) {
		ggbAPI.registerRenameListener(getId(JSFunctionName));
	};

	public String unregisterRenameListener(JSFunctionName) {
		ggbAPI.registerRenameListener(getId(JSFunctionName));
	};

	public String registerUpdateListener(JSFunctionName) {
		ggbAPI.registerUpdateListener(getId(JSFunctionName));
	};

	public String unregisterUpdateListener(JSFunctionName) {
		ggbAPI.unregisterUpdateListener(getId(JSFunctionName));
	};

	public String unregisterClientListener(JSFunctionName) {
		ggbAPI.unregisterClientListener(getId(JSFunctionName));
	};

	public String registerObjectUpdateListener(String objName, JSFunctionName) {
		ggbAPI.registerObjectUpdateListener(Ljava/lang/String;Ljava/lang/String;)(objname + "", getId(JSFunctionName));
	};

	public String unregisterObjectUpdateListener(JSFunctionName) {
		ggbAPI.unregisterObjectUpdateListener(getId(JSFunctionName));
	};

	public String registerObjectClickListener(String objName, JSFunctionName) {
		ggbAPI.registerObjectClickListener(Ljava/lang/String;Ljava/lang/String;)(objname + "", getId(JSFunctionName));
	};

	public String unregisterObjectClickListener(String objName) {
		ggbAPI.unregisterObjectClickListener(objname + "");
	};

	public String registerClickListener(JSFunctionName) {
		ggbAPI.registerClickListener(getId(JSFunctionName));
	};

	public String unregisterClickListener(JSFunctionName) {
		ggbAPI.unregisterClickListener(getId(JSFunctionName));
	};

}-*/;
}
