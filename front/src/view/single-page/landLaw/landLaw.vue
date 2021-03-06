<template>
  <div>
    <Row>
      <Col>
        <Card>
          <Form :label-width="40">
            <FormItem label="進度">
              <Progress :percent="progress">{{`${offset+1}/${count}`}}</Progress>
            </FormItem>
            <FormItem label="內容">
              <Input v-model="study.content" placeholder="..." type="textarea" :rows="4"/>
              <br>
              <ButtonGroup>
                <Button type="primary" :icon="speakButtonIcon" @click="speak">{{speakButtonText}}</Button>
                <Button type="primary" icon="ios-add" @click="fetchNewLandLaw">新增</Button>
                <Button type="primary" @click="upsert(true)">儲存</Button>
                <Button type="primary" icon="ios-trash" @click="del">刪除</Button>
              </ButtonGroup>
            </FormItem>

            <FormItem label="圖片">
              <ButtonGroup>
                <Button type="primary" icon="ios-add" @click="show_add_keyword_dlg">增加</Button>
                <Button
                  type="primary"
                  icon="ios-trash"
                  @click="del_keyword_modal=true"
                  :disabled="study.keywords.length === 0"
                >刪除</Button>
              </ButtonGroup>
            </FormItem>
            <FormItem>
              <ButtonGroup shape="circle">
                <Button @click="first" icon="ios-skip-backward"></Button>
                <Button @click="pre" icon="ios-arrow-back" :disabled="offset===0"></Button>
                <Button @click="next" icon="ios-arrow-forward" :disabled="offset+1>=count"></Button>
                <Button @click="last" icon="ios-skip-forward" :disabled="offset+1>=count"></Button>
              </ButtonGroup>
            </FormItem>
          </Form>
        </Card>
        <Modal v-model="add_keyword_modal" title="增加圖片" :footer-hide="true">
          <Form
            ref="form_add_keyword"
            :model="add_keyword"
            :closable="false"
            :rules="ruleAddKeyword"
          >
            <FormItem label="關鍵字" prop="key">
              <Input type="text" v-model="add_keyword.key" placeholder="關鍵字..."></Input>
            </FormItem>
            <FormItem label="請輸入圖片網址或上傳圖檔" prop="url">
              <Input type="url" v-model="add_keyword.url" placeholder="http://..."></Input>
            </FormItem>
            <FormItem>
              <Upload
                ref="upload"
                :action="uploadURL"
                :with-credentials="true"
                :format="['jpg','jpeg','gif']"
                :on-success="onPhotoUploadSuccess"
              >
                <Button icon="ios-cloud-upload-outline">上傳圖檔</Button>
              </Upload>
            </FormItem>
            <FormItem>
              <Button type="primary" @click="onAddKeyword">增加圖片</Button>
            </FormItem>
          </Form>
        </Modal>
        <Modal v-model="del_keyword_modal" title="刪除圖片" @on-ok="onDelKeyword">
          <Select v-model="del_keyword_idx" size="small">
            <Option
              v-for="(keyword, idx) in study.keywords"
              :value="idx"
              :key="keyword.value"
            >{{ keyword.key }}</Option>
          </Select>
        </Modal>
      </Col>
    </Row>
    <Row>
      <Col v-if="study.keywords.length">
        <Carousel v-model="keyword_idx" @on-change="onCarouselChanged">
          <CarouselItem v-for="keyword in study.keywords" :key="keyword.key">
            <Card :title="keyword.key">
              <img :src="baseUrl + keyword.url" style="width:100%;height:100%">
            </Card>
          </CarouselItem>
        </Carousel>
      </Col>
    </Row>
  </div>
</template>
<style scoped>
</style>
<script>
import {
  getLandLaw,
  getNewLandLaw,
  getLandLawCount,
  upsertLandLaw,
  delLandLaw
} from "@/api/data";
import config from "@/config";

export default {
  name: "landLaw",
  mounted() {
    this.fetchLawCount();
    this.fetchLaw();
    this.synth = window.speechSynthesis;
  },
  data() {
    return {
      offset: 0,
      count: 0,
      add_keyword_modal: false,
      photoUploaded: false,
      del_keyword_modal: false,
      del_keyword_idx: -1,
      add_keyword: {
        key: "",
        url: ""
      },
      ruleAddKeyword: {
        key: [
          {
            required: true,
            message: "請填入關鍵字",
            trigger: "blur"
          }
        ],
        url: [
          {
            required: true,
            message: "請填入圖片網址或上傳檔案",
            trigger: "blur"
          }
        ]
      },
      keyword_idx: 0,
      study: {
        _id: "000000000000000000000000",
        owner: "",
        seq: 0,
        content: "",
        keywords: []
      },
      speaking: false,
      synth: undefined
    };
  },
  computed: {
    progress() {
      return Math.ceil((this.offset * 100) / (this.count + 1));
    },
    baseUrl() {
      return process.env.NODE_ENV === "development"
        ? config.baseUrl.dev
        : config.baseUrl.pro;
    },
    uploadURL() {
      return `${this.baseUrl}photo`;
    },
    speakButtonText() {
      if (!this.synth) return "不支援語音";
      else if (this.speaking) return "中斷";
      else return "朗誦";
    },
    speakButtonIcon() {
      if (!this.synth) return "ios-mic-off";
      else if (this.speaking) return "md-pause";
      else return "ios-mic";
    }
  },
  methods: {
    fetchLaw() {
      getLandLaw({ offset: this.offset })
        .then(resp => {
          const data = resp.data;
          this.study = Object.assign({}, data);
        })
        .catch(err => {
          alert(err);
        });
    },
    fetchLawCount() {
      getLandLawCount()
        .then(resp => {
          const data = resp.data;
          this.count = data.count;
        })
        .catch(err => {
          alert(err);
        });
    },
    fetchNewLandLaw() {
      getNewLandLaw({ offset: this.offset })
        .then(resp => {
          const data = resp.data;
          this.offset += 1;
          this.count += 1;
          this.study = Object.assign({}, data);
        })
        .catch(err => {
          alert(err);
        });
    },
    first() {
      this.upsert();
      this.offset = 0;
      this.fetchLaw();
    },
    pre() {
      this.upsert();
      if (this.offset) this.offset -= 1;
      this.fetchLaw();
    },
    next() {
      this.upsert();
      this.offset += 1;
      this.fetchLaw();
    },
    last() {
      this.upsert();
      this.offset = this.count - 1;
      this.fetchLaw();
    },
    show_add_keyword_dlg() {
      this.add_keyword_modal = true;
    },
    onPhotoUploadSuccess(resp) {
      this.photoUploaded = true;
      this.add_keyword.url = `photo/${resp[0]}`;
    },
    onDelKeyword() {
      if (this.del_keyword_idx !== -1) {
        this.study.keywords.splice(this.del_keyword_idx, 1);
        this.del_keyword_idx = -1;
        this.upsert();
      }
    },
    clear_keywords() {
      this.study.keywords.splice(0, this.study.keywords.length);
    },
    onAddKeyword() {
      this.$refs["form_add_keyword"].validate(valid => {
        if (valid) {
          let new_keyword = Object.assign({}, this.add_keyword);
          this.add_keyword.key = "";
          this.add_keyword.url = "";
          this.study.keywords.push(new_keyword);
          this.$refs.upload.clearFiles();
          this.upsert();
          this.add_keyword_modal = false;
        }
      });
    },
    upsert(prompt) {
      upsertLandLaw({ study: this.study })
        .then(resp => {
          if (prompt) this.$Message.info("成功");
          this.photoUploaded = false;
        })
        .catch(err => {
          alert(err);
        });
    },
    del() {
      delLandLaw(this.study._id)
        .then(resp => {
          const data = resp.data;
          this.count -= data.count;
          if (this.offset >= this.count) this.offset = this.count - 1;

          this.fetchLaw();
        })
        .catch(err => alert(err));
    },
    speak() {
      if (this.synth) {
        let voices = this.synth.getVoices();
        let utterThis = new SpeechSynthesisUtterance(this.study.content);

        let twVoice = voices.filter(v => {
          return v.lang === "zh-TW";
        });

        if (this.synth.speaking) {
          this.synth.pause();
          this.synth.cancel();
        }

        if (this.speaking) {
          this.speaking = false;
          return;
        }

        if (twVoice.length >= 1 && !this.synth.speaking) {
          this.speaking = true;
          utterThis.voice = twVoice[0];
          utterThis.pitch = 1;
          utterThis.rate = 1;
          utterThis.onend = () => {
            this.speaking = false;
          };
          this.synth.speak(utterThis);
        }
      } else {
        alert("speechSynthesis is not supported!");
      }
    },
    onCarouselChanged(oldValue, value) {
      if (
        this.synth &&
        oldValue !== value &&
        !this.speaking &&
        !this.synth.speaking
      ) {
        let voices = this.synth.getVoices();
        let utterThis = new SpeechSynthesisUtterance(
          this.study.keywords[value].key
        );

        let twVoice = voices.filter(v => {
          return v.lang === "zh-TW";
        });

        if (twVoice.length >= 1 && !this.synth.speaking) {
          utterThis.voice = twVoice[0];
          utterThis.pitch = 1;
          utterThis.rate = 1;
          utterThis.onend = () => {
            this.speaking = false;
          };
          this.synth.speak(utterThis);
        }
      }
    }
  }
};
</script>
